open (IN, $ARGV[0]);
open (OUT, ">$ARGV[0].new");
while (my $line= <IN>) {
next if $line =~ /owl\#/;
print OUT $line;
}

