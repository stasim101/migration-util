package dbmigration;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MigrateOrganizations {
	
	Statement s;
	XSSFWorkbook res;
	
	List<CreateProjectData> createProjectDataList;
	List<UserProjectData> userProjectDataList;
	List<ProjectServiceData> projectServiceDataList;
	
	private static final String ROW_NOT_FOUND = "Row no: %d failed!";
	private static final String ENTRY_NOT_FOUND_IN_EXCEL = "Entry found in DB but not in excel for org: %d";
	private static final String ENTRY_NOT_FOUND_IN_DB = "Entry found in excel but not found in DB at row: %d";
	private static final String ENTRY_CONTAINS_EMPTY_VALUES = "Entry contains null or blank values";
	
	public MigrateOrganizations(Statement st, String createProjectPath, String userProjectPath, String projectServicePath, XSSFWorkbook results) throws IOException {		
		createProjectDataList = new ArrayList<>();
		userProjectDataList = new ArrayList<>();
		projectServiceDataList = new ArrayList<>();
		
		s = st;
		res = results;
        
		saveCreateProjectData(createProjectPath);
		saveUserProjectData(userProjectPath);
		saveProjectServiceData(projectServicePath);
		System.out.println("Scraped all data from the excel sheets!");
	}

	private void saveProjectServiceData(String filepath) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filepath));
		Sheet sheet = workbook.getSheetAt(0);
		int rowCnt = 0;
		
		for(Row row : sheet) {
			if(Objects.isNull(row.getCell(0)) 
    				|| row.getCell(0).getCellType().equals(CellType.BLANK)
        			|| Objects.isNull(row.getCell(1)) 
        			|| row.getCell(1).getCellType().equals(CellType.BLANK) 
        			|| Objects.isNull(row.getCell(2)) 
        			|| row.getCell(2).getCellType().equals(CellType.BLANK)) {
				
				System.out.println(String.format("ProjectServiceData: " + ROW_NOT_FOUND, rowCnt++));
				continue;
			}
			
			String projectName = row.getCell(0).getStringCellValue().trim();
			Integer orgId = Integer.valueOf((int)row.getCell(1).getNumericCellValue());
			String externalId = row.getCell(2).getStringCellValue().trim();
            externalId = externalId.replace("~", "");
            
            projectServiceDataList.add(new ProjectServiceData(projectName, orgId, externalId));
            rowCnt++;
		}
		
		workbook.close();
	}

	private void saveUserProjectData(String filepath) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filepath));
		Sheet sheet = workbook.getSheetAt(0);
		int rowCnt = 0;
		
		for(Row row : sheet) {
			if(Objects.isNull(row.getCell(0)) 
    				|| row.getCell(0).getCellType().equals(CellType.BLANK)
        			|| Objects.isNull(row.getCell(1)) 
        			|| row.getCell(1).getCellType().equals(CellType.BLANK) 
        			|| Objects.isNull(row.getCell(2)) 
        			|| row.getCell(2).getCellType().equals(CellType.BLANK)
        			|| Objects.isNull(row.getCell(3)) 
        			|| row.getCell(3).getCellType().equals(CellType.BLANK)) {
				
				System.out.println(String.format("UserProjectData: " + ROW_NOT_FOUND, rowCnt++));
				continue;
			}
			
			String projectName = row.getCell(0).getStringCellValue().trim();
			Integer orgId = Integer.valueOf((int)row.getCell(1).getNumericCellValue());
            String userId = row.getCell(2).getStringCellValue().trim();
            String role = row.getCell(3).getStringCellValue().trim();
            
            userProjectDataList.add(new UserProjectData(projectName, orgId, userId, role));
            rowCnt++;
		}
		
		workbook.close();
	}

	private void saveCreateProjectData(String filepath) throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filepath));
		Sheet sheet = workbook.getSheetAt(0);
		int rowCnt = 0;
		
		for(Row row : sheet) {
			if(Objects.isNull(row.getCell(0)) 
    				|| row.getCell(0).getCellType().equals(CellType.BLANK)
        			|| Objects.isNull(row.getCell(1)) 
        			|| row.getCell(1).getCellType().equals(CellType.BLANK) 
        			|| Objects.isNull(row.getCell(2)) 
        			|| row.getCell(2).getCellType().equals(CellType.BLANK)
        			|| Objects.isNull(row.getCell(3)) 
        			|| row.getCell(3).getCellType().equals(CellType.BLANK)) {
				
				System.out.println(String.format("CreateProjectData: " + ROW_NOT_FOUND, rowCnt++));
				continue;
			}
			
			String projectName = row.getCell(0).getStringCellValue().trim();
			Integer orgId = Integer.valueOf((int)row.getCell(1).getNumericCellValue());
            String userId = row.getCell(2).getStringCellValue().trim();
            String projectType = row.getCell(3).getStringCellValue().trim();
            
            createProjectDataList.add(new CreateProjectData(projectName, orgId, userId, projectType));
            rowCnt++;
		}
		
		workbook.close();
	}

	public void validateData() throws SQLException {
		System.out.println("Validation started...");
		validateAllOrgOwners();
		validateAllOrgsPresent();
		validateAllUserPresent();
		validateProjectService();
		System.out.println("Validation complete!");
	}

	private void validateProjectService() throws SQLException {
		Set<String> dbServices = getDatabaseServices();
		List<Pair<String, String>> invalidEntries = new ArrayList<>();
		Set<String> toBeRemoved = new HashSet<>();
		
		for(int i=0; i<projectServiceDataList.size(); i++) {
			if(dbServices.contains(projectServiceDataList.get(i).getExternalId())) {
				//dbServices.remove(projectServiceDataList.get(i).getExternalId());
				toBeRemoved.add(projectServiceDataList.get(i).getExternalId());
			}
			else {
				String message = String.format(ENTRY_NOT_FOUND_IN_DB, i+1);
				invalidEntries.add(Pair.create(projectServiceDataList.get(i).getExternalId(), message));
			}
		}
		
		for(String id : toBeRemoved) {
			dbServices.remove(id);
		}
		
		if(!dbServices.isEmpty()) {
			for(String service : dbServices) {
				String message = String.format("Service found in DB but not in excel for external id: %s", service);
				invalidEntries.add(Pair.create(service, message));
			}
		}
		
		printInResultsSheet("validate-all-services-prosent", invalidEntries);
		System.out.println("All Services verified!");
	}

	private Set<String> getDatabaseServices() throws SQLException {
		Set<String> servicesSet = new HashSet<>();
		
		String serviceQuery = "SELECT external_id from service where organization_id is not null";
		ResultSet serviceResultSet = s.executeQuery(serviceQuery);
		
		if(!serviceResultSet.isBeforeFirst()) {
			System.out.println("No services found in DB!");
			return servicesSet;
		}
		
		while(serviceResultSet.next()) {
			servicesSet.add(serviceResultSet.getString(1));
		}
		
		return servicesSet;
	}

	private void validateAllUserPresent() throws SQLException {
		Set<Pair<String, Integer>> dbUsers = getDatabaseUsers();
		List<Pair<String, String>> invalidEntries = new ArrayList<>();
		
		for(int i=0; i<userProjectDataList.size(); i++) {
			if(dbUsers.contains(Pair.create(userProjectDataList.get(i).getUserId(), userProjectDataList.get(i).getOrgId()))) {
				dbUsers.remove(Pair.create(userProjectDataList.get(i).getUserId(), userProjectDataList.get(i).getOrgId()));
			}
			else {
				String message = String.format(ENTRY_NOT_FOUND_IN_DB, i+1);
				invalidEntries.add(Pair.create(userProjectDataList.get(i).getUserId(), message));
			}
		}
		
		if(!dbUsers.isEmpty()) {
			for(Pair<String, Integer> user : dbUsers) {
				String message = String.format(ENTRY_NOT_FOUND_IN_EXCEL, user.getValue());
				invalidEntries.add(Pair.create(user.getKey(), message));
			}
		}
		
		printInResultsSheet("validation-all-users-present", invalidEntries);
		System.out.println("All Organization Users verified!");
	}

	private Set<Pair<String, Integer>> getDatabaseUsers() throws SQLException {
		Set<Pair<String, Integer>> usersSet = new HashSet<>();
		
		String userQuery = "SELECT o.id, ru.user_id FROM organization o, user_organization uo, registered_user ru where o.id = uo.organization_id and uo.registered_user_id = ru.id and uo.role in ('ADMIN', 'MEMBER')";
		ResultSet userResultSet = s.executeQuery(userQuery);
		
		if(!userResultSet.isBeforeFirst()) {
			System.out.println("No org users found in DB!");
			return usersSet;
		}
		
		while(userResultSet.next()) {
			usersSet.add(Pair.create(userResultSet.getString(2), userResultSet.getInt(1)));
		}

		return usersSet;
	}

	private void validateAllOrgsPresent() throws SQLException {
		Set<Integer> dbOrgs = getDatabaseOrgs();
		List<Pair<String, String>> invalidEntries = new ArrayList<>();
		Set<Integer> toBeRemoved = new HashSet<>();
		
		for(int i=0; i<createProjectDataList.size(); i++) {
			if(dbOrgs.contains(createProjectDataList.get(i).getOrgId())) {
				// dbOrgs.remove(createProjectDataList.get(i).getOrgId());
				toBeRemoved.add(createProjectDataList.get(i).getOrgId());
			}
			else {
				String message = String.format(ENTRY_NOT_FOUND_IN_DB, i+1);
				invalidEntries.add(Pair.create(createProjectDataList.get(i).getOrgId().toString(), message));
			}
		}
		
		for(Integer id : toBeRemoved) {
			dbOrgs.remove(id);
		}
		
		if(!dbOrgs.isEmpty()) {
			for(Integer id : dbOrgs) {
				String message = String.format(ENTRY_NOT_FOUND_IN_EXCEL, id);
				invalidEntries.add(Pair.create(id.toString(), message));
			}
		}
		
		printInResultsSheet("validate-all-orgs-present", invalidEntries);
		System.out.println("All Organization verified!");
	}

	private Set<Integer> getDatabaseOrgs() throws SQLException {
		Set<Integer> dbOrgs = new HashSet<>();
		
		String getAllOrgsQuery = "select id from organization";
		ResultSet orgSet = s.executeQuery(getAllOrgsQuery);
		
		if(!orgSet.isBeforeFirst()) {
			System.out.println("No orgs in DB!");
			return dbOrgs;
		}
		
		while(orgSet.next()) {
			dbOrgs.add(orgSet.getInt(1));
		}
		
		return dbOrgs;
	}

	private void validateAllOrgOwners() throws SQLException {
		Set<Pair<String, Integer>> dbOrgOwners = getDatabaseOrgOwners();
		List<Pair<String, String>> invalidEntries = new ArrayList<>();
		
		for(int i=0; i<createProjectDataList.size(); i++) {
			if(dbOrgOwners.contains(Pair.create(createProjectDataList.get(i).getOwnerUserId(), createProjectDataList.get(i).getOrgId()))) {
				dbOrgOwners.remove(Pair.create(createProjectDataList.get(i).getOwnerUserId(), createProjectDataList.get(i).getOrgId()));
			}
			else {
				String message = String.format(ENTRY_NOT_FOUND_IN_DB + 
						", Not an OrgOwner but wants to be ProjectOwner", i+1);
				invalidEntries.add(Pair.create(createProjectDataList.get(i).getOwnerUserId(), message));
			}
		}
		
		if(!dbOrgOwners.isEmpty()) {
			for(Pair<String, Integer> orgOwner : dbOrgOwners) {
				String message = String.format(ENTRY_NOT_FOUND_IN_EXCEL + 
						", OrgOwner but entry not present in CreateProject", orgOwner.getValue());
				invalidEntries.add(Pair.create(orgOwner.getKey(), message));
			}
		}
		
		printInResultsSheet("validation-all-org-owners", invalidEntries);
		System.out.println("All Organization Owners verified!");
	}

	private void printInResultsSheet(String sheetName, List<Pair<String, String>> invalidEntries) {
		Sheet sheet = res.createSheet(sheetName);
		
		for(int i=0; i<invalidEntries.size(); i++) {
			Row row = sheet.createRow(i);
        	Cell cell = row.createCell(0);
        	cell.setCellValue(invalidEntries.get(i).getKey());
        	Cell message = row.createCell(1);
        	message.setCellValue(invalidEntries.get(i).getValue());
		}
	}

	private Set<Pair<String, Integer>> getDatabaseOrgOwners() throws SQLException {
		Set<Pair<String, Integer>> orgOwnersSet = new HashSet<>();
		
		String orgOwnerQuery = "SELECT o.id, ru.user_id FROM organization o, user_organization uo, registered_user ru where o.id = uo.organization_id and uo.registered_user_id = ru.id and uo.role = 'OWNER'";
		ResultSet orgOwnerSet = s.executeQuery(orgOwnerQuery);
		
		if(!orgOwnerSet.isBeforeFirst()) {
			System.out.println("No org owners found");
			return orgOwnersSet;
		}
		
		while(orgOwnerSet.next()) {
			orgOwnersSet.add(Pair.create(orgOwnerSet.getString(2), orgOwnerSet.getInt(1)));
		}
		
		return orgOwnersSet;
	}
	
	public void migrateCreateProject() throws SQLException {		
		List<Pair<String, String>> rowLog = new ArrayList<>();
		
		for(int i=0; i<createProjectDataList.size(); i++) {
			String curProjectName = createProjectDataList.get(i).getProjectName();
			Integer curOrgId = createProjectDataList.get(i).getOrgId();
			String curOwnerUserId = createProjectDataList.get(i).getOwnerUserId();
			String curProjectType = createProjectDataList.get(i).getProjectType();
			
			// check for null values
			if(createProjectDataList.get(i).containsEmptyValues()) {
				rowLog.add(Pair.create(createProjectDataList.get(i).toString(), ENTRY_CONTAINS_EMPTY_VALUES));
				continue;
			}
			
			// check if user exists -> get regUserId
			Pair<Boolean, String> userExists = checkIfUserExists(curOwnerUserId);
			if(Boolean.FALSE.equals(userExists.getFirst())) {
				rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "User does not exist!"));
				continue;
			}
			
			String regUserId = userExists.getSecond();
			
			// check if org exists -> already have orgId
			Pair<Boolean, String> orgExists = checkIfOrgExists(createProjectDataList.get(i).getOrgId());
			if(Boolean.FALSE.equals(orgExists.getFirst())) {
				rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "Organization does not exist!"));
				continue;
			}
			
			// check if user part of the org
			Boolean userPartOfOrg = checkIfUserPartOfOrganization(regUserId, createProjectDataList.get(i).getOrgId());
			if(Boolean.FALSE.equals(userPartOfOrg)) {
				rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "User is not part of the organization!"));
				continue;
			}
			
			// check if project name used in the org
			Pair<Boolean, Pair<String, String>> projectExists = checkIfProjectExists(createProjectDataList.get(i).getProjectName(), createProjectDataList.get(i).getOrgId());
			if(Boolean.TRUE.equals(projectExists.getFirst())) {
				rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "Project with name already exists in the organization!"));
				continue;
			}
			
			// check if projectType is valid
			if(Boolean.FALSE.equals(curProjectType.equals("FOLDER") 
					|| curProjectType.equals("TEMPLATE") || curProjectType.equals("CONSOLE_TEMPLATE"))) {
				rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "Project type is not valid!"));
				continue;
			}
			
			// count the number of projects in the org
			Integer defaultProject = (countProjectsInOrg(curOrgId) == 0) ? 1 : 0;
			
			// insert project into the table
			String query = String.format("insert into project(name, project_type, status, organization_id, created_by, updated_by, description, default_project) values ('%s', '%s', 'LAUNCHED', %d, '%s', '%s', '%s', %d)", 
					curProjectName, 
					curProjectType,
					curOrgId, 
					curOwnerUserId, 
					curOwnerUserId, 
					curProjectName, 
					defaultProject);
			
            s.executeUpdate(query);
			
			// get project id
            Pair<Boolean, Pair<String, String>> newProjectExists = checkIfProjectExists(createProjectDataList.get(i).getProjectName(), createProjectDataList.get(i).getOrgId());
            if(Boolean.FALSE.equals(newProjectExists.getFirst())) {
            	rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "Project insertion failed using sql query!"));
				continue;
            }
            
            String projectId = newProjectExists.getSecond().getFirst();
			
			// insert user project into the table
            String userOrgQuery = String.format("insert into user_project(role, project_id, "
            		+ "registered_user_id, state, created_by, updated_by) "
            		+ "values ('OWNER', %s, '%s', 'confirmed', '%s', '%s')", 
            		projectId, regUserId, createProjectDataList.get(i).getOwnerUserId(), 
            		createProjectDataList.get(i).getOwnerUserId());
            
            s.executeUpdate(userOrgQuery);
			
			// insert projectSettings in org table
            String metadata = "{\"projectSettings\": {\"createProject\": false}}";
            String orgQuery = String.format("update organization set metadata = '%s' where id = %s", metadata, createProjectDataList.get(i).getOrgId());
            s.executeUpdate(orgQuery);
            
            rowLog.add(Pair.create(createProjectDataList.get(i).toString(), "SUCCESS"));
		}
		
		printInResultsSheet("create-project-log", rowLog);
		System.out.println("Completed migrating create-project!");
	}

	private Integer countProjectsInOrg(Integer orgId) throws SQLException {
		String query = String.format("select count(id) from project where organization_id = %d", orgId);
		ResultSet resultSet = s.executeQuery(query);
		
		Integer count = 0;
		if(resultSet.next()) {
			count = resultSet.getInt(1);
		}
		
		return count;
	}

	private Pair<Boolean, Pair<String, String>> checkIfProjectExists(String projectName, Integer orgId) throws SQLException {
		ResultSet projectResultSet = s.executeQuery("select id, created_by from project where name = '" + projectName + "' and organization_id = " + orgId);
        
        if(!projectResultSet.isBeforeFirst()) {
        	return Pair.create(Boolean.FALSE, Pair.create("", ""));
        }
        
        String projectId = null;
        String projectOwnerUserId = null;
        if(projectResultSet.next()) {
        	projectId = projectResultSet.getString(1);
        	projectOwnerUserId = projectResultSet.getString(2);
        }
        
        return Pair.create(Boolean.TRUE, Pair.create(projectId, projectOwnerUserId));
	}

	private Boolean checkIfUserPartOfOrganization(String regUserId, Integer orgId) throws SQLException {
		ResultSet userOrganizationSet = 
				s.executeQuery("select id from user_organization where registered_user_id = '" + regUserId + "' and organization_id = " + orgId);
        
        return userOrganizationSet.next();
	}

	private Pair<Boolean, String> checkIfOrgExists(Integer orgId) throws SQLException {
		ResultSet orgResultSet = s.executeQuery("select name from organization where id = " + orgId);
        
        if(!orgResultSet.isBeforeFirst()) {
        	return Pair.create(Boolean.FALSE, "");
        }
        
        String orgName = null;
        if(orgResultSet.next()) {
        	orgName = orgResultSet.getString(1);
        }
        
        return Pair.create(Boolean.TRUE, orgName);
	}

	private Pair<Boolean, String> checkIfUserExists(String userId) throws SQLException {
		ResultSet userResultSet = 
        		s.executeQuery("select id from registered_user where user_id = '" + userId + "'");
        
        if(!userResultSet.isBeforeFirst()) {
        	return Pair.create(Boolean.FALSE, "");
        }
        
        String regUserId = null;
        if(userResultSet.next()) {
        	regUserId = userResultSet.getString(1);
        }
        
        return Pair.create(Boolean.TRUE, regUserId);
	}

	public void migrateUserProject() throws SQLException {
		List<Pair<String, String>> rowLog = new ArrayList<>();
		
		for(int i=0; i<userProjectDataList.size(); i++) {
			String curProjectName = userProjectDataList.get(i).getProjectName();
			Integer curOrgId = userProjectDataList.get(i).getOrgId();
			String curUserId = userProjectDataList.get(i).getUserId();
			String curProjectRole = userProjectDataList.get(i).getProjectRole();
			
			// check for empty values
			if(userProjectDataList.get(i).containsEmptyValues()) {
				rowLog.add(Pair.create(userProjectDataList.get(i).toString(), ENTRY_CONTAINS_EMPTY_VALUES));
				continue;
			}
			
			// check if project exists -> get projectId and projectOwnerUserId
			Pair<Boolean, Pair<String, String>> projectExists = checkIfProjectExists(curProjectName, curOrgId);
			if(Boolean.FALSE.equals(projectExists.getFirst())) {
				rowLog.add(Pair.create(userProjectDataList.get(i).toString(), "Project does not exist!"));
				continue;
			}
			
			String projectId = projectExists.getSecond().getFirst();
			String projectOwnerUserId = projectExists.getSecond().getSecond();
			
			// check if user exists
			Pair<Boolean, String> userExists = checkIfUserExists(curUserId);
			if(Boolean.FALSE.equals(userExists.getFirst())) {
				rowLog.add(Pair.create(userProjectDataList.get(i).toString(), "User does not exist!"));
				continue;
			}
			
			String regUserId = userExists.getSecond();
			
			// check if organization exists
			Pair<Boolean, String> orgExists = checkIfOrgExists(curOrgId);
			if(Boolean.FALSE.equals(orgExists.getFirst())) {
				rowLog.add(Pair.create(userProjectDataList.get(i).toString(), "Organization does not exist!"));
				continue;
			}
			
			// check if role is admin or member
			if(!curProjectRole.equals("ADMIN") && !curProjectRole.equals("MEMBER")) {
				rowLog.add(Pair.create(userProjectDataList.get(i).toString(), "Project role should be ADMIN or MEMBER"));
				continue;
			}
			
			// update user project
			String query = String.format("insert into user_project(role, project_id, state, registered_user_id, created_by, updated_by) values ('%s', '%s', 'confirmed', '%s', '%s', '%s')", curProjectRole, projectId, regUserId, projectOwnerUserId, projectOwnerUserId);
            s.executeUpdate(query);
			
            rowLog.add(Pair.create(userProjectDataList.get(i).toString(), "SUCCESS"));
		}
		
		printInResultsSheet("link-user-project-log", rowLog);
		System.out.println("Completed migrating user-project-mapping!");
		
	}

	public void migrateLinkServiceProject() throws SQLException {
		List<Pair<String, String>> rowLog = new ArrayList<>();
		
		for(int i=0; i<projectServiceDataList.size(); i++) {
			String curProjectName = projectServiceDataList.get(i).getProjectName();
			Integer curOrgId = projectServiceDataList.get(i).getOrgId();
			String curExternalId = projectServiceDataList.get(i).getExternalId(); 
			
			// check null values
			if(projectServiceDataList.get(i).containsEmptyValues()) {
				rowLog.add(Pair.create(projectServiceDataList.get(i).toString(), ENTRY_CONTAINS_EMPTY_VALUES));
				continue;
			}
			
			// check if service present -> get service id
			Pair<Boolean, String> serviceExists = checkIfServiceExists(curExternalId);
			if(Boolean.FALSE.equals(serviceExists.getFirst())) {
				rowLog.add(Pair.create(projectServiceDataList.get(i).toString(), "Service does not exist!"));
				continue;
			}
			
			String serviceId = serviceExists.getSecond();
			
			// check if project exists with name -> get projectid and project owner userid
			Pair<Boolean, Pair<String, String>> projectExists = checkIfProjectExists(curProjectName, curOrgId);
			if(Boolean.FALSE.equals(projectExists.getFirst())) {
				rowLog.add(Pair.create(projectServiceDataList.get(i).toString(), "Project does not exist!"));
				continue;
			}
			
			String projectId = projectExists.getSecond().getFirst();
			String projectOwnerUserId = projectExists.getSecond().getSecond();
			
			// get user service metadata
			String metadata = checkIfMetadataExist(serviceId);
			
			// get project owner regUserId
			Pair<Boolean, String> userExists = checkIfUserExists(projectOwnerUserId);
			if(Boolean.FALSE.equals(userExists.getFirst())) {
				rowLog.add(Pair.create(projectServiceDataList.get(i).toString(), "Project Owner does not exist!"));
				continue;
			}
			
			String projectOwnerRegUserId = userExists.getSecond();
			
			// update service
			String updateServiceQuery = String.format("update service set project_id = '%s', state = 'CREATED', created_by = '%s', updated_by = '%s', metadata = '%s' where external_id = '%s'", projectId, projectOwnerRegUserId, projectOwnerRegUserId, metadata, curExternalId);
            s.executeUpdate(updateServiceQuery);
            
            rowLog.add(Pair.create(projectServiceDataList.get(i).toString(), "SUCCESS"));
		}
		
		printInResultsSheet("link-project-service-log", rowLog);
		System.out.println("Completed migrating project-service-mapping!");

	}

	private String checkIfMetadataExist(String serviceId) throws SQLException {
		String metadataQuery = String.format("select meta_data from user_service where service_id = %s and meta_data is not null limit 1", serviceId);
        ResultSet metadataResult = s.executeQuery(metadataQuery);
        
        String metadata = "{}";
        if(metadataResult.next()) {
        	metadata = metadataResult.getString(1);
        }
        
        return metadata;
	}

	private Pair<Boolean, String> checkIfServiceExists(String externalId) throws SQLException {
		ResultSet serviceResultSet = s.executeQuery("select id from service where external_id = '" + externalId + "'");
        
        if(!serviceResultSet.isBeforeFirst()) {
        	return Pair.create(Boolean.FALSE, "");
        }
        
        String serviceId = null;
        if(serviceResultSet.next()) {
        	serviceId = serviceResultSet.getString(1);
        }
        
        return Pair.create(Boolean.TRUE, serviceId);
	}

	public void updateApikeys() throws SQLException {
		List<String> regUserIds = getUsersWithEmptyApiKeys();
		
		for(int i=0; i<regUserIds.size(); i++) {
			String apiKey = RandomStringUtils.randomAlphanumeric(32).toLowerCase();
			String apiKeyQuery = String.format("update registered_user set api_key = '%s' where id = '%s'", apiKey, regUserIds.get(i));
			s.executeUpdate(apiKeyQuery);
		}
		
		System.out.println("Completed updating apikey in users!");
	}

	private List<String> getUsersWithEmptyApiKeys() throws SQLException {
		List<String> regUserIds = new ArrayList<>();
		
		String query = "SELECT id FROM registered_user where api_key is null";
		ResultSet userIdSet = s.executeQuery(query);
		
		if(!userIdSet.isBeforeFirst()) {
			return regUserIds;
		}
		
		while(userIdSet.next()) {
			regUserIds.add(userIdSet.getString(1));
		}
		
		return regUserIds;
	}

}




/*

		public void migrateCreateProject(Statement st, String filepath, XSSFWorkbook results) {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filepath));
	        Sheet sheet = workbook.getSheetAt(0);
	        System.out.println(":::::::::::Migrating Create Projects::::::::::");
	        int rowCnt = 0;
	        
	        Sheet resultCreateProject = results.createSheet("create_project");
	        
	        for (Row row : sheet) {	            
	            try {
	            	// check if values are null or blank
	            	if(Objects.isNull(row.getCell(0)) 
	        				|| row.getCell(0).getCellType().equals(CellType.BLANK)
		        			|| Objects.isNull(row.getCell(1)) 
		        			|| row.getCell(1).getCellType().equals(CellType.BLANK) 
		        			|| Objects.isNull(row.getCell(2)) 
		        			|| row.getCell(2).getCellType().equals(CellType.BLANK)) {
	            		String errorMessage = "NULL/BLANK values encountered!";
	            				
	            		System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultCreateProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
		            }
	            	
	            	// read values from the row
	                String projectName = row.getCell(0).getStringCellValue().trim();
	                int orgId = (int)row.getCell(1).getNumericCellValue();
	                String userId = row.getCell(2).getStringCellValue().trim();
	                
	                // check if user exists
	                ResultSet userResultSet = 
	                		st.executeQuery("select id from registered_user where user_id = '" + userId + "'");
	                
	                if(!userResultSet.isBeforeFirst()) {
	                	String errorMessage = String.format("User not found with userId: %s", userId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultCreateProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                String regUserId = null;
	                if(userResultSet.next()) {
	                	regUserId = userResultSet.getString(1);
	                }
	                
	                // check if organization exists
	                ResultSet orgResultSet = st.executeQuery("select name from organization where id = '" + orgId + "'");
	                
	                if(!orgResultSet.isBeforeFirst()) {
	                	String errorMessage = String.format("Organization not found with orgId: %s", orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultCreateProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                // check if user is part of the organization
	                ResultSet userOrganizationSet = st.executeQuery("select id from user_organization where registered_user_id = '" + regUserId + "' and organization_id = '" + orgId + "'");
	                
	                if(!userOrganizationSet.isBeforeFirst()) {
	                	String errorMessage = String.format("User: %s not found with orgId: %s", userId, orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultCreateProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                // check if project-name exists in the organization
	                ResultSet projectResultSet = st.executeQuery("select id from project where name = '" + projectName + "' and organization_id = '" + orgId + "'");
	                
	                if(projectResultSet.isBeforeFirst()) {
	                	String errorMessage = String.format("Project with name: %s already exists is org: %s", projectName, orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultCreateProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                // insert project into the table
	                String query = String.format("insert into project(name, project_type, status, organization_id, created_by, updated_by, description) values ('%s', 'FOLDER', 'LAUNCHED', %d, '%s', '%s', '%s')", projectName, orgId, userId, userId, projectName);
	                st.executeUpdate(query);
	                
	                // get the id of the project
	                ResultSet newProjectSet = st.executeQuery("select id from project where name = '" + projectName + "' and organization_id = '" + orgId + "'");
	                
	                // insert user-project into the table
	                if(newProjectSet.next()) {
	                	String projectId = newProjectSet.getString(1);
	                	String userOrgQuery = String.format("insert into user_project(role, project_id, registered_user_id, state, created_by, updated_by) values ('OWNER', %s, '%s', 'confirmed', '%s', '%s')", projectId, regUserId, userId, userId);
		                st.executeUpdate(userOrgQuery);
	                }
	                else {
	                	String errorMessage = String.format("Project not found with name: %s and orgId: %s", projectName, orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultCreateProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                // insert projectSettings metadata into organization table
	                String metadata = "{\"projectSettings\": {\"createProject\": false}}";
	                String orgQuery = String.format("update organization set metadata = '%s' where id = %s", metadata, orgId);
	                st.executeUpdate(orgQuery);
	                
	                System.out.println("Row no: " + rowCnt + " completed!");
	                
	                Row header = resultCreateProject.createRow(rowCnt);
                	Cell headerCell = header.createCell(0);
                	headerCell.setCellValue("Success");
	                
	            } catch (Exception e) {
	            	System.out.println("Row no: " + rowCnt + " failed!");
	                System.err.println(e);
	                
	                Row header = resultCreateProject.createRow(rowCnt);
                	Cell headerCell = header.createCell(0);
                	headerCell.setCellValue("Failure");
                	
                	Cell errorCell = header.createCell(1);
                	errorCell.setCellValue(e.getMessage());
	            }
	            rowCnt++;
	        }
	        
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		} 		
	}

	public void migrateUserProject(Statement st, String filepath, XSSFWorkbook results) throws FileNotFoundException, IOException {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filepath));
	        Sheet sheet = workbook.getSheetAt(0);
	        System.out.println(":::::::::::Migrating User Projects::::::::::");
	        int rowCnt = 0;
	        
	        Sheet resultUserProject = results.createSheet("link-user-project");
	       
	        for (Row row : sheet) {
	            try {
	            	// check if values are null
	            	if(Objects.isNull(row.getCell(0))
	        				|| row.getCell(0).getCellType().equals(CellType.BLANK)
		        			|| Objects.isNull(row.getCell(1))
		        			|| row.getCell(1).getCellType().equals(CellType.BLANK)
		        			|| Objects.isNull(row.getCell(2))
		        			|| row.getCell(2).getCellType().equals(CellType.BLANK)
		        			|| Objects.isNull(row.getCell(3))
		        			|| row.getCell(3).getCellType().equals(CellType.BLANK)) {
	            		String errorMessage = "NULL/BLANK values encountered!";
	            		
	            		System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultUserProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
		            }
	            	
	                String projectName = row.getCell(0).getStringCellValue().trim();
	                int orgId = (int)row.getCell(1).getNumericCellValue();
	                String userId = row.getCell(2).getStringCellValue().trim();
	                String role = row.getCell(3).getStringCellValue().trim();

	                // check if project exists
	                ResultSet projectResultSet = st.executeQuery("SELECT id, created_by FROM project where organization_id = "+ orgId +" and name = '"+ projectName +"'");
	               
	                if(!projectResultSet.isBeforeFirst()) {
	                	String errorMessage = String.format("Project not found with projectName: %s and orgId: %s", projectName, orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultUserProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	               
	                String projectId = null;
	                String projectOwneUserId = null;
	                if(projectResultSet.next()) {
	                	projectId = projectResultSet.getString(1);
	                	projectOwneUserId = projectResultSet.getString(2);
	                }
	               
	                // check if user exists
                	ResultSet userResultSet = st.executeQuery("SELECT id FROM registered_user where user_id = '"+ userId +"'");
	               
                	if(!userResultSet.isBeforeFirst()) {
                		String errorMessage = String.format("User not found with userId: %s", userId);
                		
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultUserProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
                	
                	String regUserId = null;
                	if(userResultSet.next()) {
                		regUserId = userResultSet.getString(1);
                	}
                	
                	//checking if organization exists
                	ResultSet orgResultSet = st.executeQuery("SELECT id FROM organization where id = " + orgId);
                	if(!orgResultSet.isBeforeFirst()) {
                		String errorMessage = String.format("Organization not found with orgId %s", orgId);
                		
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultUserProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
                	
                	// role provided must be ADMIN or MEMBER
                	if(!role.equals("ADMIN") && !role.equals("MEMBER")) {
                		String errorMessage = "Role must be either ADMIN or MEMBER";
                		
                		System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultUserProject.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
                	}
  
	                String query = String.format("insert into user_project(role, project_id, state, registered_user_id, created_by, updated_by) values ('%s', '%s', 'confirmed', '%s', '%s', '%s')", role, projectId, regUserId, projectOwneUserId, projectOwneUserId);
	                st.executeUpdate(query);
	               
	                System.out.println("Row no: " + rowCnt + " success!");
	                
	                Row header = resultUserProject.createRow(rowCnt);
                	Cell headerCell = header.createCell(0);
                	headerCell.setCellValue("Success");
	            }
	            catch (Exception e) {
	            	System.out.println("Row no: " + rowCnt + " failed!");
	                System.err.println(e);
	                
	                Row header = resultUserProject.createRow(rowCnt);
                	Cell headerCell = header.createCell(0);
                	headerCell.setCellValue("Failure");
                	
                	Cell errorCell = header.createCell(1);
                	errorCell.setCellValue(e.getMessage());
	            }
	            rowCnt++;
	        }
	       
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		} 				
	}

	public void migrateLinkServiceProject(Statement st, String filepath, XSSFWorkbook results) {
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filepath));
			Sheet sheet = workbook.getSheetAt(0);
	        System.out.println(":::::::::::Migrating Link Service and Project::::::::::");
	        int rowCnt = 0;
	        
	        Sheet resultLinkService = results.createSheet("link-service-project");
	        
	        for (Row row : sheet) {
	        	try {
	        		// check if values are null
	        		if(Objects.isNull(row.getCell(0)) 
	        				|| row.getCell(0).getCellType().equals(CellType.BLANK)
		        			|| Objects.isNull(row.getCell(1)) 
		        			|| row.getCell(1).getCellType().equals(CellType.BLANK) 
		        			|| Objects.isNull(row.getCell(2)) 
		        			|| row.getCell(2).getCellType().equals(CellType.BLANK)) {
	        			String errorMessage = "NULL/BLANK values encountered!";
	        			
	        			System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultLinkService.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
		        	}
	        		
	        		// read values from the row
	                String projectName = row.getCell(0).getStringCellValue().trim();
	                int orgId = (int)row.getCell(1).getNumericCellValue();
	                String externalId = row.getCell(2).getStringCellValue().trim();
	                externalId = externalId.replace("~", "");
	                
	                // check if service present
	                ResultSet serviceResultSet = st.executeQuery("select id from service where external_id = '" + externalId + "'");
	                
	                if(!serviceResultSet.isBeforeFirst()) {
	                	String errorMessage = String.format("Service not found with externalId: %s", externalId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultLinkService.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                String serviceId = null;
	                if(serviceResultSet.next()) {
	                	serviceId = serviceResultSet.getString(1);
	                }
	                
	                // check if project with name projectName exists in org
	                ResultSet projectResult = st.executeQuery("select id, created_by from project where name = '"+ projectName + "' and organization_id = " + orgId);
	                
	                if(!projectResult.isBeforeFirst()) {
	                	String errorMessage = String.format("Project not found with name: %s and orgId: %s", projectName, orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultLinkService.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                String projectId = null;
	                String projectOwnerUserId = null;
	                if(projectResult.next()) {
	                	projectId = projectResult.getString(1);
	                	projectOwnerUserId = projectResult.getString(2);
	                }
	                
	                String metadataQuery = String.format("select meta_data from user_service where service_id = %s and meta_data is not null limit 1", serviceId);
	                ResultSet metadataResult = st.executeQuery(metadataQuery);
	                
	                String metadata = null;
	                if(metadataResult.next()) {
	                	metadata = metadataResult.getString(1);
	                }
	                
	                String projectOwnerQuery = String.format("select id from registered_user where user_id = '%s'", projectOwnerUserId);
	                ResultSet projectOwnerSet = st.executeQuery(projectOwnerQuery);
	                
	                String projectOwnerRegUserId = null;
	                if(projectOwnerSet.next()) {
	                	projectOwnerRegUserId = projectOwnerSet.getString(1);
	                }  
	                
	                if(Objects.isNull(projectId) || Objects.isNull(projectOwnerUserId) || Objects.isNull(projectOwnerRegUserId)) {
	                	String errorMessage = String.format("Project not found with name: %s and orgId: %s", projectName, orgId);
	                	
	                	System.out.println("Row no: " + rowCnt + " failed!");
	                	System.out.println(errorMessage);
	                	
	                	Row header = resultLinkService.createRow(rowCnt);
	                	Cell headerCell = header.createCell(0);
	                	headerCell.setCellValue("Failure");
	                	
	                	Cell errorCell = header.createCell(1);
	                	errorCell.setCellValue(errorMessage);
	                	
	                	rowCnt++;
	                	continue;
	                }
	                
	                String updateServiceQuery = String.format("update service set project_id = '%s', state = 'CREATED', created_by = '%s', updated_by = '%s', metadata = '%s' where external_id = '%s'", projectId, projectOwnerRegUserId, projectOwnerRegUserId, metadata, externalId);
                    st.executeUpdate(updateServiceQuery);
                    
                    System.out.println("Row no: " + rowCnt + " completed!");
	                
	                Row header = resultLinkService.createRow(rowCnt);
                	Cell headerCell = header.createCell(0);
                	headerCell.setCellValue("Success");
	                
	            } catch (Exception e) {
	            	System.out.println("Row no: " + rowCnt + " failed!");
	                System.err.println(e);
	                
	                Row header = resultLinkService.createRow(rowCnt);
                	Cell headerCell = header.createCell(0);
                	headerCell.setCellValue("Failure");
                	
                	Cell errorCell = header.createCell(1);
                	errorCell.setCellValue(e.getMessage());
	            }
	        	rowCnt++;
	        }
	        
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	
	public void migrateOrganizations(Statement st, String filePath) throws IOException {
	    String logo = "https://ik.imagekit.io/cuhjahvkf/Org_NLnW0YkVS.png?ik-sdk-version=javascript-1.4.3&updatedAt=1659438087266";
	    XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filePath));
	    Sheet sheet = workbook.getSheetAt(0);
	    Map<Integer, List<String>> data = new HashMap();
	    System.out.println("Migrating Organizations::::::::::");
	//    try {
	//        st.executeUpdate("DELETE FROM organization WHERE id=1");
	//    } catch (Exception e) {
	//        System.err.println("error while remving the org1" + e);
	//    }
	    int orgId = 2;
	    for (Row row : sheet) {
	        try {
	            String orgName = row.getCell(0).getStringCellValue().trim();
	            String userId = row.getCell(1).getStringCellValue().trim();
	            ResultSet resultSet = st.executeQuery("SELECT id from registered_user WHERE user_id='" + userId + "'");
	            if (resultSet.next()) {
	                String regUserId = resultSet.getString(1);
	                st.executeUpdate("INSERT INTO organization(id, name, logo, owner_id, created_at, state_updated_at) " + "VALUES (" + orgId + ", '" + orgName + "', '" + logo + "', '" + regUserId + "', now(), now())");
	                UUID uuid = UUID.randomUUID();
	                st.executeUpdate("INSERT INTO user_organization(id, registered_user_id, organization_id, state, role, created_at, state_updated_at) " + "VALUES ('" + uuid + "', '" + regUserId + "', '" + orgId + "', 'confirmed', 'OWNER', now(), now())");
	                orgId++;
	            }
	        } catch (Exception e) {
	            System.err.println("Got an exception while migrating orgs: " + e);
	        }
	    }
	}

    public void migrateUserOrganizations(Statement st, String filePath) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filePath));
        Sheet sheet = workbook.getSheetAt(0);
        System.out.println("Migrating User Organizations::::::::::");
        for (Row row : sheet) {
            try {
                String orgName = row.getCell(0).getStringCellValue().trim();
                String userId = row.getCell(1).getStringCellValue().trim();

                ResultSet orgResultSet = st.executeQuery("SELECT id from organization WHERE name='" + orgName + "'");
                if (orgResultSet.next()) {
                    String orgId = orgResultSet.getString(1);
                    ResultSet userResultSet = st.executeQuery("SELECT id from registered_user WHERE user_id='" + userId + "'");
                    if (userResultSet.next()) {
                        String regUserId = userResultSet.getString(1);
                        UUID uuid = UUID.randomUUID();
                        st.executeUpdate("INSERT INTO user_organization(id, registered_user_id, organization_id, state, role, created_at, state_updated_at) " + "VALUES ('" + uuid + "', '" + regUserId + "', '" + orgId + "', 'confirmed', 'MEMBER', now(), now())");
                    }
                }
            } catch (Exception e) {
                System.err.println("Got an exception while migrating user orgs: " + e);
            }
        }
    }
    
        public void migrateOrgServices(Statement st, String filePath) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filePath));
        Sheet sheet = workbook.getSheetAt(0);
        System.out.println("Migrating Organization Services::::::::::");
        for (Row row : sheet) {
            try {
                String orgName = row.getCell(0).getStringCellValue().trim();
                String externalId = row.getCell(1).getStringCellValue().trim();
                externalId = externalId.replace("~", "");
                ResultSet orgResultSet = st.executeQuery("SELECT id from organization WHERE name='" + orgName + "'");
                if (orgResultSet.next()) {
                    String orgId = orgResultSet.getString(1);
                    st.executeUpdate("UPDATE service SET organization_id ='" + orgId + "' WHERE external_id='" + externalId + "'");
                }
            } catch (Exception e) {
                System.err.println("Got an exception while migrating org services: " + e);
            }
        }

    }

    public void migrateUserServices(Statement st) throws IOException, SQLException {
        System.out.println("Migrating User Services::::::::::");
        st.executeUpdate("UPDATE user_service SET role='ADMIN'");
    }

*/