# OSGi-Utils

Collection of utilities useful in OSGi environments.

## configadmin-sql-persistence

This module provides a SQL database-backed persistence manager for Apache Felix Configuration Admin implementation. Configuration properties are stored in a single table as text in a format similar to the one used by the default file persistence manager.

The module will attempt to create the required table if not present in the database. If you prefer to create the table manually or by other means, the create statement used internally is the following: 

	CREATE TABLE <schema.><table> (pid VARCHAR NOT NULL, prop_name VARCHAR NOT NULL, prop_type VARCHAR NOT NULL, prop_value VARCHAR, CONSTRAINT pk_<table> PRIMARY KEY (pid, prop_name)) `

**Remark**
This module was only tested with PostgreSQL 

##### Configuration
This module is configured using the system properties listed below. If you are using Apache Karaf they can be set in `etc/custom.properties`.

| Name                                      | Default value | Description                                           |
| :---                                      | :-----:       | :-------------                                        |
|`com.jassuncao.osgi.cm.sql.driver.name`    | `nulll`  | JDBC driver name                                      |
|`com.jassuncao.osgi.cm.sql.url`            | `null`        | JDBC URL to use to connect to the database            |
|`com.jassuncao.osgi.cm.sql.user`           | `null`        | The database user in the connection to the database   |
|`com.jassuncao.osgi.cm.sql.password`       | `null`        | The user's password                                   |
|`com.jassuncao.osgi.cm.sql.dataSourceName` | `cm_sql`      | The name of the data source that will created         |
|`com.jassuncao.osgi.cm.sql.table`          | `osgi_config` | The table where the properties should be kept         |
|`com.jassuncao.osgi.cm.sql.schema`         | `null`        | The table's database schema                           | 
|`com.jassuncao.osgi.cm.sql.loglevel`       | `2`           | Log level where 1 corresponds to error and 4 to debug | 

In addition to these properties is necessary to set an additional property that will instruct  Felix Configuration Admin to use a different persistence manager.
This property is named `felix.cm.pm` and must be set to `sql`.

**Example**

	felix.cm.pm=sql
	com.jassuncao.osgi.cm.sql.driver.name=PostgreSQL JDBC Driver
	com.jassuncao.osgi.cm.sql.url=jdbc:postgresql://localhost/database
	com.jassuncao.osgi.cm.sql.user=johndoe
	com.jassuncao.osgi.cm.sql.password=password
	com.jassuncao.osgi.cm.sql.table=osgi_config
	com.jassuncao.osgi.cm.sql.schema=my_schema

