#!/usr/bin/perl -w



# Check incoming file.

($#ARGV == 0) || die "ERROR: Supply exactly ONE file for parsing!\n";
$lineCoordFile = $ARGV[0];
(-e $lineCoordFile) || die "ERROR: File $lineCoordFile doesn't exist!\n";

# Read the file

open (FILE, "< $lineCoordFile") || die "ERROR: Cannot open $lineCoordFile!\n";
@fileContent = <FILE>;
close (FILE);

@intervalArray = ();
@gapArray=();

$curSensor = 0;

  @curSensorData = ();
foreach $line (@fileContent)
{
  # Extract the y-coordinate of the intervals (ignore the x-coordinate).
  if ($line =~ /sor(\d+) \[[-+]?[0-9]*\.?[0-9]+,([-+]?[0-9]*\.?[0-9]+)\] \[[-+]?[0-9]*\.?[0-9]+,([-+]?[0-9]*\.?[0-9]+)\]/){
    # If it's a new sensor, store the old sensor into intervalArray
    if ($1 != $curSensor)
    {
	# Storing reference of the old sensor
        $intervalArray[$curSensor] = \@curSensorData ;
	# shift to new sensor
        @curSensorData = ();
        $curSensor = $1;
	push(@curSensorData, ($2, $3));
    }
    # Otherwise, just add on the old sensor
    else 
    {
	push(@curSensorData, ($2, $3));
    }
  }
}
# Store the data from the last sensor into the interval array
$intervalArray[$curSensor] = [ @curSensorData ];

$maxSensor = $curSensor;

# generate the gap array
@gapArray=();
$minCoord = -50000; $maxCoord = 50000;
for ($i=0; $i<$maxSensor; $i++){
  $curSensordata = $intervalArray[$i];
  @curGapData = ($minCoord);
  for ($j=0; $j<$#{$curSensorData}; $j++){push(@curGapData, $intervalArray[$i][$j]);}
  push(@curGapData, $maxCoord);
  $gapArray[$i] = \@curGapData;
}


# loop through every sensor
for ($i=0; $i<$maxSensor; $i++){
  $curSensorData = $intervalArray[$i];
  $nextSensorData = $intervalArray[$i+1];
  $curSensor = $i;
  $nextSensor = $i+1;
  
  # loop through every interval
  for ($j=0; $j<$#{$curSensorData}; $j+=2){
    # get the start and end of this interval
    $intervalIndex = $j/2+1;
    $intervalStart = $intervalArray[$i][$j];
    $intervalEnd = $intervalArray[$i][$j+1];
   
     # loop through every interval of the next sensor
    for ($k=0; $k<$#{$nextSensorData}; $k+=2){
      # get start, end and index of this interval
      $newIntervalIndex = $k/2+1;
      $newIntervalStart = $intervalArray[$i+1][$k];
      $newIntervalEnd = $intervalArray[$i+1][$k+1];

      # identify the IA relation between the two intervals. 
      print "Sensor$curSensor\_Interval$intervalIndex ";
      printIARelation($intervalStart,$intervalEnd,$newIntervalStart,$newIntervalEnd);
      print " Sensor$nextSensor\_Interval$newIntervalIndex; ";
    }
    print "\n";
  }

}




####

# New negative component

# New positive component

# End negative component

# End positive component

# Continue negative component

# Continuing positive component. 
# interval relation s, si, f, fi, d, di, o, oi 


# Subroutines

sub printIARelation{

  $firstStart = $_[0];
  $firstEnd = $_[1];
  $secondStart = $_[2];
  $secondEnd = $_[3];

  if ($firstEnd < $secondStart){
	print "<";
  }
  if ($firstStart > $secondEnd){
	print ">";
  }

  if ($firstEnd==$secondStart){print "m";}
  if ($firstStart==$secondEnd){print "mi";}

  if ($firstStart<$secondStart){
	if ($secondStart < $firstEnd){
		if ($firstEnd < $secondEnd){print "o";}
		if ($secondEnd < $firstEnd){print "di";}
	}
  }
  if ($secondStart<$firstStart){
	if ($firstStart < $secondEnd){
		if ($secondEnd < $firstEnd){print "oi";}
		if ($firstEnd < $secondEnd){print "d";}
	}
  }


  if ($firstStart == $secondStart){
	if ($firstEnd == $secondEnd){
		print "=";
	}
	elsif ($firstEnd < $secondEnd){
		print "s";
	}
	else {print "si";}
  }

  if ($firstEnd == $secondEnd){
	if ($firstStart > $secondStart) {print "f";}
	if ($firstStart < $secondStart) {print "fi";}
  }

}
