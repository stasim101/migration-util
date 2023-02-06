package dbmigration;

import java.util.Objects;

import org.apache.logging.log4j.util.Strings;

public class CreateProjectData {

	String projectName;
	Integer orgId;
	String ownerUserId;
	String projectType;
	
	public CreateProjectData(String projectName, Integer orgId, String ownerUserId, String projectType) {
		super();
		this.projectName = projectName;
		this.orgId = orgId;
		this.ownerUserId = ownerUserId;
		this.projectType = projectType;
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

	public String getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	public String getProjectType() {
		return projectType;
	}

	public void setProjectType(String projectType) {
		this.projectType = projectType;
	}

	public boolean containsEmptyValues() {
		return Strings.isBlank(this.projectName) || Objects.isNull(this.orgId) 
				|| Strings.isBlank(this.ownerUserId) || Strings.isBlank(projectType);
	}

	@Override
	public String toString() {
		return "CreateProjectData [projectName=" + projectName + ", orgId=" + orgId + ", ownerUserId=" + ownerUserId
				+ ", projectType=" + projectType + "]";
	}
}
