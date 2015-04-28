-- MySQL dump 10.13  Distrib 5.6.19, for linux-glibc2.5 (x86_64)
--
-- Host: localhost    Database: xdjob
-- ------------------------------------------------------
-- Server version	5.5.40-0ubuntu0.14.04.1-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `MY_XD_JOB_MOD_DS`
--

DROP TABLE IF EXISTS `MY_XD_JOB_MOD_DS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MY_XD_JOB_MOD_DS` (
  `JOB_DEF_NAME` varchar(45) NOT NULL,
  `URL` varchar(45) NOT NULL,
  `USERNAME` varchar(45) NOT NULL,
  `PASSWORD` varchar(45) NOT NULL,
  `DRIVER` varchar(45) NOT NULL DEFAULT 'com.mysql.jdbc.Driver',
  `EXPORT_DIR` varchar(200) DEFAULT NULL,
  `EXPORT_FILE_EXTENSION` varchar(25) NOT NULL,
  `IMPORT_DIR` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`JOB_DEF_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MY_XD_JOB_MOD_DS`
--

LOCK TABLES `MY_XD_JOB_MOD_DS` WRITE;
/*!40000 ALTER TABLE `MY_XD_JOB_MOD_DS` DISABLE KEYS */;
INSERT INTO `MY_XD_JOB_MOD_DS` VALUES ('tenant2backuponly','jdbc:mysql://localhost:3306/datasource1','root','mysql','com.mysql.jdbc.Driver','/home/justin/just-dev/xd-dist/xd-customized-singlenode-setup/xd-out','json',''),
('tenant2restoreonly','jdbc:mysql://localhost:3306/datasource2','root','mysql','com.mysql.jdbc.Driver','','json','/home/justin/just-dev/xd-dist/xd-customized-singlenode-setup/xd-in');
/*!40000 ALTER TABLE `MY_XD_JOB_MOD_DS` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-04-18 23:08:06
