package dbmigration;

import java.util.Objects;

import org.apache.logging.log4j.util.Strings;

public class UserProjectData {
	
	String projectName;
	Integer orgId;
	String userId;
	String projectRole;
	
	public UserProjectData(String projectName, Integer orgId, String userId, String projectRole) {
		super();
		this.projectName = projectName;
		this.orgId = orgId;
		this.userId = userId;
		this.projectRole = projectRole;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Integer getOrgId() {
		return orgId;
	}

	public void setOrgId(Integer orgId) {
		this.orgId = orgId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProjectRole() {
		return projectRole;
	}

	public void setProjectRole(String projectRole) {
		this.projectRole = projectRole;
	}
	
	public boolean containsEmptyValues() {
		return Strings.isBlank(this.projectName) || Strings.isBlank(this.userId) 
				|| Strings.isBlank(this.projectRole) || Objects.isNull(this.orgId);
	}

	@Override
	public String toString() {
		return "UserProjectData [projectName=" + projectName + ", orgId=" + orgId + ", userId=" + userId
				+ ", projectRole=" + projectRole + "]";
	}
	
}
