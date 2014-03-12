==============================
SensorNetworkTestCaseGenerator
==============================

To generate random polygons with multilayers of holes and sub-regions


##TO GENERATE REGIONS

Run command: 
      
      	USAGE: java -jar RegionGenerator.jar parameters...
				parameters:
					width=<Integer>
					height=<Integer>
					nCases=<Integer>
					nSensorSets=<Integer>
					gap=<Integer>
Example:

      java -jar regionGenerator.jar nCases=20
      
20 cases will be generated and stored in ./data folder with default settings.
