USE test;

CREATE TABLE `DemoTestGroups` (
  `GroupName` varchar(80) NOT NULL,
  `Description` varchar(200) NOT NULL,
  `GroupValue` varchar(80) NOT NULL,
  PRIMARY KEY (`GroupValue`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `DemoTestUsers` (
  `Name` varchar(80) NOT NULL,
  `FirstName` varchar(80) NOT NULL,
  `LastName` varchar(80) NOT NULL,
  `EmployeeID` varchar(80) NOT NULL,
  `EmailAddress` varchar(100) NOT NULL,
  `NetworkID` varchar(50) NOT NULL,
  `GroupValue` varchar(20) NOT NULL,
  KEY `GroupValue` (`GroupValue`),
 FOREIGN KEY (`GroupValue`) REFERENCES `DemoTestGroups` (`GroupValue`)
) ENGINE=InnoDB DEFAULT CHARSET=latSELECT `demotestusers`.`Name`,
    `demotestusers`.`FirstName`,
    `demotestusers`.`LastName`,
    `demotestusers`.`EmployeeID`,
    `demotestusers`.`EmailAddress`,
    `demotestusers`.`NetworkID`,
    `demotestusers`.`GroupValue`
FROM `test`.`demotestusers`;
in1;
INSERT INTO `test`.`demotestgroups`(`GroupName`,`Description`,`GroupValue`)
VALUES ('DeveAccess','eng','DevAccess');
INSERT INTO `test`.`demotestusers`
(`Name`,
`FirstName`,
`LastName`,
`EmployeeID`,
`EmailAddress`,
`NetworkID`,
`GroupValue`)
VALUES
('James Smith', 'James', 'Smith', '100','jamessmith@gmail.com','james.smith','Finance');

Delete from demotestusers where EmployeeID='100'

SELECT `demotestgroups`.`GroupName`,
    `demotestgroups`.`Description`,
    `demotestgroups`.`GroupValue`
FROM `test`.`demotestgroups`;




SELECT `demotestusers`.`Name`,
    `demotestusers`.`FirstName`,
    `demotestusers`.`LastName`,
    `demotestusers`.`EmployeeID`,
    `demotestusers`.`EmailAddress`,
    `demotestusers`.`NetworkID`,
    `demotestusers`.`GroupValue`
FROM `test`.`demotestusers`;



