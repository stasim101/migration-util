package dbmigration;

import java.util.Objects;

import org.apache.logging.log4j.util.Strings;

public class ProjectServiceData {

	String projectName;
	Integer orgId;
	String externalId;
	
	public ProjectServiceData(String projectName, Integer orgId, String externalId) {
		super();
		this.projectName = projectName;
		this.orgId = orgId;
		this.externalId = externalId;
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

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	public boolean containsEmptyValues() {
		return Strings.isBlank(this.projectName) 
				|| Strings.isBlank(this.externalId) || Objects.isNull(this.orgId);
	}
	
	@Override
	public String toString() {
		return "ProjectServiceData [projectName=" + projectName + ", orgId=" + orgId + ", externalId=" + externalId
				+ "]";
	}
	
}
