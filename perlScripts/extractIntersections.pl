#!/opt/local/bin/perl5 -w


use Math::Intersection::StraightLine;
use Data::Dumper;
my $finder = Math::Intersection::StraightLine->new();




# one intersection point
#   my $vector_a = [[20,60],[-40,0]];
#   my $vector_b = [[50,80],[0,50]];
#   my $result = $finder->vectors($vector_a,$vector_b);
#   print Dumper($result);

# no intersection point
#   my $point_a = [[20,60],[30,10]];
#   my $point_b = [[50,80],[50,75]];
#   $result = $finder->point_limited($point_a,$point_b);
#   print Dumper($result); 


# Reading in files

($#ARGV == 1) || die "ERROR: Supply exactly TWO file for parsing!\n";

$file1 = $ARGV[0]; $file2 = $ARGV[1];

($intervals1, $gaps1) = getIntervals($file1);
($intervals2, $gaps2) = getIntervals($file2);



sub getIntervals{
    my $lineCoordFile = $_;
    my $intervalArray = ();
    my $gapArray = ();
    my $curSensor = 0;
    my $curSensorData = ();

    open (FILE, "< $lineCoordFile") || die "ERROR: Cannot open $lineCoordFile!\n";
    @fileContent = <FILE>;
    close(FILE);
    foreach $line (@fileContent){
	# Extract the y-coordinate of the intervals (ignore the x-coordinate).
	if ($line =~ /sor(\d+) \[[-+]?[0-9]*\.?[0-9]+,([-+]?[0-9]*\.?[0-9]+)\] \[[-+]?[0-9]*\.?[0-9]+,([-+]?[0-9]*\.?[0-9]+)\]/){
	    # get the right variables from the parsing. 
	    $sensorNumber= $1; $x1 = $2; $y1 = $3; $x2 = $4; $y2 = $5;	
	    # If it's a new sensor, store the old sensor into intervalArray
	    if ($1 != $curSensor){
	        $intervalArray[$curSensor] = \@curSensorData;
		@curSensorData = ();
		$curSensor = $sensorNumber;
		push(@curSensorData, (($x1, $y1), ($x2, $y2)));
	    }
	    # Otherwise, just add on the old sensor
	    else{
		push(@curSensorData, (($x1,$y1), ($x2,$y2)));	
	    }
	}
    }
    # Store the data from the last sensor into the interval array
    $intervalArray[$curSensor] = \@curSensorData ;

    my $maxSensor = $curSensor;

    my $minCoord =-50000; my $maxCoord = 50000;

    for ($i=0; $i<$maxSensor; $i++){
	@curSensorData = @{$intervalArray[$i]};
	@curGapData = ($minCoord);
	for ($j=0; $j<$#{$curSensorData}; $j++){

		push(@curGapData, $intervalArray[$i][$j]);
	}
	push(@curGapData, $maxCoord);
	$gapArray[$i] = \@curGapData;
    } 

    return (\@intervalArray, \@gapArray);
}	
