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

| Name                                      | Default value | Description 
| :---                                      | :-----:       | :------------- 
|`com.jassuncao.osgi.cm.sql.driver.name`    | `null`        | JDBC driver name  
|`com.jassuncao.osgi.cm.sql.url`            | `null`        | JDBC URL to use to connect to the database            
|`com.jassuncao.osgi.cm.sql.user`           | `null`        | The database user in the connection to the database
|`com.jassuncao.osgi.cm.sql.password`       | `null`        | The user's password
|`com.jassuncao.osgi.cm.sql.table`          | `osgi_config` | The table where the properties should be kept
|`com.jassuncao.osgi.cm.sql.schema`         | `null`        | The table's database schema
|`com.jassuncao.osgi.cm.sql.loglevel`       | `2`           | Log level where 1 corresponds to error and 4 to debug 
|`com.jassuncao.osgi.cm.sql.paxconfig`      | `null`        | An optional Pax JDBC datasource configuration file [^1] 

[^1]: Properties defined as system properties take precedence over the ones defined in Pax JDBC properties. 
Only `osgi.jdbc.driver.name`, `url`, `user` and `password` are used.

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

### Delegated persistence

This module also includes a delegated persistence manager where the actual persistence is delegated to two other persistence managers, a primary and a secondary. The decision between one of the two managers  is made using a regular expression. This expression determines the PIDs that should be kept in the primary persistence manager. 
The delegated persistence manager can be used to segregate configurations,  where configurations matching a PID pattern are stored in database and others in file or memory.
To use this persistence manager one must set the system property  `felix.cm.pm`to `delegated` and set the system properties listed below.

| Name                                          | Default value | Description 
| :---                                          | :-----:       | :-------------  
|`com.jassuncao.osgi.cm.delegated.primary`      | `null`        | The name of the primary persistence manager [^2]
|`com.jassuncao.osgi.cm.delegated.secondary`    | `null`        | The name of the secondary persistence manager [^2]
|`com.jassuncao.osgi.cm.delegated.primary.pids` | `null`        | Regular expression used to filter the PIDs that should be stored in the primary persistence manager  |

[^2]: One of the following values `sql`, `file`, `memory`

**Example**

	felix.cm.pm=delegated
	com.jassuncao.osgi.cm.sql.paxconfig=etc/org.ops4j.datasource-dsName.cfg
    com.jassuncao.osgi.cm.sql.table=osgi_config
    com.jassuncao.osgi.cm.sql.schema=osgi_config
    com.jassuncao.osgi.cm.delegated.primary=sql
    com.jassuncao.osgi.cm.delegated.secondary=memory
    com.jassuncao.osgi.cm.delegated.primary.pids=^com\.acme.*
