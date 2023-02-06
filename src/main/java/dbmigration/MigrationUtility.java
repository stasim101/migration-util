package dbmigration;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Objects;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MigrationUtility {
	
	private static final String LOCAL_DB1_URL = "jdbc:mysql://localhost:3306/ucm_test?autoReconnect=true&useSSL=false";
    //private static final String LOCAL_DB2_URL = "jdbc:mysql://localhost:3306/ucm_phase3?autoReconnect=true&useSSL=false";

    private static final String LOCAL_DB_URL = "jdbc:mysql://localhost:3306/ucm_prod_dump?autoReconnect=true&useSSL=false";
    
    private static final String DEV_DB_URL = "jdbc:mysql://localhost:3306/ucm_test?autoReconnect=true&useSSL=false";
    private static final String QA_DB_URL = "jdbc:mysql://bots-uniq-qa-mstdb.cluster-caxos9nbfqlh.ap-south-1.rds.amazonaws.com/unified?enabledTLSProtocols=TLSv1.2";
    private static final String PROD_DB_URL = "jdbc:mysql://unification-mstdb-instance-1.c8sj9qmedvr7.ap-south-1.rds.amazonaws.com/ucm?enabledTLSProtocols=TLSv1.2";
    
    private static final String LOCAL_USER_NAME = "root";
    private static final String LOCAL_PWD = "root";
    
    private static final String DEV_USER_NAME = "appuser";
    private static final String DEV_PWD = "Admin@321";
    
    private static final String QA_USER_NAME = "readwrite";
    private static final String QA_PWD = "EawnUmOnd";
    
    private static final String PROD_USER_NAME = "appuser";
    private static final String PROD_PWD = "A9p@uz3r6UcM22";

	public static void main(String[] args) {
		try {
        	String env = args[0];
        	String createProjectSheetPath = args[1];
        	String linkUserProjectSheetPath = args[2];
        	String linkProjectServiceSheetPath = args[3];
        	String fileLocation = args[4];
        	String verificationFlag = args[5];
        	
            String url = LOCAL_DB1_URL;
            String user = LOCAL_USER_NAME;
            String pwd = LOCAL_PWD;
            
            if(Objects.nonNull(env)) {
            	if(env.equalsIgnoreCase("dev")){
            		url = DEV_DB_URL;
                    user = DEV_USER_NAME;
                    pwd = DEV_PWD;
            	} else if(env.equalsIgnoreCase("qa")) {
            		url = QA_DB_URL;
                    user = QA_USER_NAME;
                    pwd = QA_PWD;
            	} else if(env.equalsIgnoreCase("prod")) {
            		url = PROD_DB_URL;
                    user = PROD_USER_NAME;
                    pwd = PROD_PWD;
            	}
            }
            
            System.out.println("Starting DB migration...");

            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            Connection conn = DriverManager.getConnection(url, user, pwd);
            Statement st = conn.createStatement();
            XSSFWorkbook results = new XSSFWorkbook();
            
            MigrateOrganizations migrateOrganizations = new MigrateOrganizations(st, createProjectSheetPath, linkUserProjectSheetPath, linkProjectServiceSheetPath, results);
            
            if(verificationFlag.equals("verify")) {
            	migrateOrganizations.validateData();
            }
            else if(verificationFlag.equals("migrate")) {
            	migrateOrganizations.migrateCreateProject();
                migrateOrganizations.migrateUserProject();
                migrateOrganizations.migrateLinkServiceProject();
                migrateOrganizations.updateApikeys();
            }
                       
            FileOutputStream outputStream = new FileOutputStream(fileLocation);
            results.write(outputStream);
            results.close();
            
            conn.close();
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e);
        }
	}
}
