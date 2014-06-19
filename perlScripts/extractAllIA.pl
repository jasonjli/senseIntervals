#!/usr/bin/perl -w

my $dir = "./data";

opendir (DIR, $dir) || die $!;

while (my $file = readdir(DIR)){
  if ($file =~ /norm/){
    `./extractIA.pl $dir/$file > $dir/$file.ia.txt`;
  }
}
