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

$curSensor = 0;

  @curSensorData = ();
foreach $line (@fileContent)
{
  if ($line =~ /sor(\d+) \[[-+]?[0-9]*\.?[0-9]+,([-+]?[0-9]*\.?[0-9]+)\] \[[-+]?[0-9]*\.?[0-9]+,([-+]?[0-9]*\.?[0-9]+)\]/){
    if ($1 != $curSensor)
    {
        $intervalArray[$curSensor] = [ @curSensorData ];
        @curSensorData = ();
        $curSensor = $1;
	push(@curSensorData, ($2, $3));
    }
    else 
    {
	push(@curSensorData, ($2, $3));
    }
  }
}

$intervalArray[$curSensor] = [ @curSensorData ];

for ($i=0; $i<=$curSensor; $i++){
  $iSensorData = $intervalArray[$i];
  print "Sensor$i ";
  for ($j=0; $j<=$#{$iSensorData}; $j++){
    print "$intervalArray[$i][$j] ";
  }
  print "\n"

}
