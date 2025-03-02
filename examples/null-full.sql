SELECT
	e.ReportsTo AS reportID,
	e.EmployeeId AS eID
FROM
	Employee e
ORDER BY
	eID
