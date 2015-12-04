-- Install extensions
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_topology;
CREATE EXTENSION pgrouting;
SET search_path = topology,public;

-- Part I: Create topology for 'roads' with postgis_topology
SELECT topology.CreateTopology('roads_topo', 4326);
SELECT topology.AddTopoGeometryColumn('roads_topo', 'public', 'roads', 'topo_geom', 'LINESTRING');

-- Create Topology Layer 1 with tolerance 0.000007
DO $$DECLARE r record;
BEGIN
  FOR r IN SELECT * FROM roads LOOP
    BEGIN
	  UPDATE roads SET topo_geom = topology.toTopoGeom(geom, 'roads_topo', 1, 0.000007)
      WHERE gid = r.gid;
    EXCEPTION
      WHEN OTHERS THEN
        RAISE WARNING 'Loading of record % failed: %', r.osm_id, SQLERRM;
    END;
  END LOOP;
END$$;

-- Verify Topology
SELECT * FROM
    topology.TopologySummary('roads_topo'); 

-- PartII: Create topology with pgRouting (for shortest path calculation)

ALTER TABLE roads ADD COLUMN "source" integer;
ALTER TABLE roads ADD COLUMN "target" integer;

SELECT pgr_createTopology('roads', 0.00001, 'geom', 'gid');

-- Add columns for cost and reverse cost
ALTER TABLE roads ADD COLUMN "cost" double precision;
ALTER TABLE roads ADD COLUMN "reverse_cost" double precision;

-- Fill cost and reverse cost with data
UPDATE roads SET
cost = ST_Length(geom),
reverse_cost= ST_Length(geom);

--Calculate shortest path between id 323 and 100
SELECT seq, id1 AS node, id2 AS edge, cost FROM pgr_dijkstra('
SELECT gid AS id,
source::integer,
target::integer,
cost::double precision
FROM roads',
100, 323, false, false
);
