-- DROP FUNCTION find_nearest_road(double precision,double precision);

-- Function to find the nearest road to the given coordinates
CREATE OR REPLACE FUNCTION find_nearest_road
(IN latitude double precision, IN longitude double precision) -- input parameters: coordinates as longitude and latitude values
RETURNS TABLE -- structure of output
(
	road_id integer,		-- road id
	name character varying,		-- road name (e.g. Moltkestraße)
	ref character varying,		-- road reference (e.g. B 54)
	distance integer		-- distance to the nearest road
) AS $$
BEGIN
RETURN QUERY
	SELECT roads.gid AS road_id, roads.name, roads.ref, CAST (st_distance_sphere(roads.geom, st_setsrid(st_makepoint(longitude, latitude), 4326)) AS INT) AS distance
	FROM roads
	ORDER BY roads.geom <-> st_setsrid(st_makepoint(longitude, latitude), 4326) -- geometric operator <-> means "distance between"
	LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Demo
SELECT find_nearest_road(51.955584, 7.623015);