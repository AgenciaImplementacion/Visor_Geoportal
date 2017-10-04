package it.gesp.geoportal.dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "system_settings", uniqueConstraints = { @UniqueConstraint(columnNames = { "id" }) })
public class SystemSetting {

	private int idSystemSetting;
	private String configKey;
	private String configValue;
	
	public SystemSetting() {
		
	}
	
	public SystemSetting(String key, String value) {
		this.configKey = key;
		this.configValue = value;
	}
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public int getIdSystemSetting() {
		return idSystemSetting;
	}

	public void setIdSystemSetting(int idSystemSetting) {
		this.idSystemSetting = idSystemSetting;
	}

	@Column(name = "key", unique=true, nullable=false)
	public String getConfigKey() {
		return configKey;
	}

	public void setConfigKey(String configKey) {
		this.configKey = configKey;
	}

	@Column(name = "value", unique=true, nullable=false)
	public String getConfigValue() {
		return configValue;
	}

	public void setConfigValue(String configValue) {
		this.configValue = configValue;
	}
}