SELECT
	e.ReportsTo AS reportID,
	e.EmployeeId AS eID
FROM
	Employee e
WHERE
	e.ReportsTo IS NOT NULL
ORDER BY
	eID
