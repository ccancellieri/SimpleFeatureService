SELECT 
  dt_gaul.uuid_gaul
  --dt_gaul.at_geom
FROM 
  gaul.dt_gaul
WHERE
     dt_gaul.at_admlevel=0
     AND
     ST_Intersects(
        dt_gaul.at_geom,
        -- INCOMING GEOMETRY -- EXAMPLE --
	ST_setSRID(
		(SELECT
		ST_Extent(dt_gaul.at_geom)
		FROM 
		gaul.dt_gaul
		WHERE
		dt_gaul.at_gaul_l0 IN (0,1,2,3)
		),
		4326)
	-- EXAMPLE END
	)
     

SELECT 
  dt_gaul.uuid_gaul, 
  dt_gaul.at_geom
FROM 
  gaul.dt_gaul
WHERE
 dt_gaul.pk_gaul IN (111,11,22,4)


SELECT 
  --dt_gaul.uuid_gaul, 
  --dt_gaul.pk_gaul, 
  --dt_gaul.at_admlevel, 
  --dt_gaul.at_gaul_l0, 
  --dt_gaul.at_gaul_l1, 
  --dt_gaul.at_gaul_l2,
  --dt_gaul.at_geom,
  ST_Extent(dt_gaul.at_geom) as bextent
FROM 
  gaul.dt_gaul
WHERE
  dt_gaul.at_gaul_l0 IN (0,1,2,3)