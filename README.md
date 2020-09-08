# sqlitediff
Utility to facilitate fast feedback for learning SQL on a known SQLite database.

Usage: `java -jar SQLiteDiff <answer file> <query file>`
where ...
- `answer file` is a CSV representing the desired output of the query
- `query file` is a text file containing only SQL to run

The utility reads the query file, executes it on a bundled SQLite database, and then compares rows/columns to the contents of the answer file, providing a detailed account of differences.

Note: the included database is currently Chinook [https://github.com/lerocha/chinook-database] but could theoretically be any SQLite database.

Tested on latest versions of macOS, Ubuntu, and Windows using Oracle/Open JDK 8 - 14.
