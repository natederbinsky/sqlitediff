SELECT 
	alb.Title AS albTitle,
	t.Name AS trackName,
	t.Composer AS trackComposer
FROM
	((Artist a INNER JOIN Album alb ON a.ArtistId=alb.ArtistId)
	INNER JOIN Track t ON alb.AlbumId=t.AlbumId)
	INNER JOIN Genre g ON t.GenreId=g.GenreId
WHERE
	a.name = "Various Artists" AND 
	t.Composer IS NOT NULL AND
	g.Name = "Latin"
ORDER BY
	trackComposer