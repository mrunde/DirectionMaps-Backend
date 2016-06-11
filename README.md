DirectionMaps-Backend
=======
Backend of the App-Group from the DirectionMaps study project at the Institute for Geoinformatics, Münster

## Database setup (car only)

### PostGreSQL

* install extensions:
```
-- Install extensions
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_topology;
CREATE EXTENSION pgrouting;
SET search_path = topology,public;
```


### OSM2PO

* download OSM2PO 5.0.0 [link](osm2po.de)
* download an OSM XML file, for example from geofabrik [link](http://download.geofabrik.de/europe/germany/nordrhein-westfalen/muenster-regbez.html)
* pick {file}.osm.bz2 and extract the file
* (OPTIONAL) use osmosis to extract a subset of that file `osmosis --read-xml muenster-regbez-latest.osm.pbf -bb left=7.459302 right=7.764173 top=52.020917 bottom=51.876174 --write-xml muenster.osm`
* replace your `osm2po.config` with `DirectionMaps-Backend/database/osm2po.config`
* run OSM2PO: `java -jar osm2po-core-5.0.0-signed.jar prefix=ms {file}.osm`
* (keep OSM2PO open and test if OSM2PO created a valid topology at `http://localhost:8888/Osm2poService`)
* this will create a SQL file in directory "ms"
* import this file into PostGreSQL using psq:l
* `psql -U postgres -d {database_name} -q -f "{path_to_sql_file}"` (a commandline template is also available in the created log file)
* (run twice if table ms_2po_4pgr doesn't exist already)
* rename database to `roads_pgr`
* test your database with:

```
SELECT seq, id1 AS node, id2 AS edge, cost FROM pgr_dijkstra('
SELECT gid AS id,
source::integer,
target::integer,
cost::double precision
FROM roads_pgr',
100, 323, false, false
);
```



## Run Algorithm
```java -jar dm-alg.jar lat lng transportationType filenameSuffix```
