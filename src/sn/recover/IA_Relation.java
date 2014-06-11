package sn.recover;

public class IA_Relation {
	private short relation;
	
	public static final int eq = 1;
	public static final int p = 2;
	public static final int pi = 4;
	public static final int m = 8;
	public static final int mi = 16;
	public static final int s = 32;
	public static final int si = 64;
	public static final int o = 128;
	public static final int oi = 256;
	public static final int d = 512;
	public static final int di = 1024;
	public static final int f = 2048;
	public static final int fi = 4096;
	
	public static final int U = 8191;
	
	// empty constructor constructor
	public IA_Relation(){
		relation = IA_Relation.U;		
	}
	
	public int getRel(){
		return relation;
	}
	
	public IA_Relation(double firstStart, double firstEnd, double secondStart, double secondEnd){
		
		// assert that the starts are before ends
		assert (firstStart<firstEnd && secondStart<secondEnd);
		
		relation = 0;
		
		if (firstEnd < secondStart) relation = IA_Relation.p;
		else if (secondEnd < firstStart) relation = IA_Relation.pi;
		else if (firstEnd == secondStart) relation = IA_Relation.m;
		else if (secondEnd == firstStart) relation = IA_Relation.mi;
		else if (firstStart < secondStart){
			if (firstEnd < secondEnd) relation = IA_Relation.o;
			if (firstEnd == secondEnd) relation = IA_Relation.fi;
			if (firstEnd > secondEnd) relation = IA_Relation.di;
		}
		else if (firstStart == secondStart){
			if (firstEnd < secondEnd) relation = IA_Relation.s;
			if (firstEnd == secondEnd) relation = IA_Relation.eq;
			if (firstEnd > secondEnd) relation = IA_Relation.si;
		}
		else if (firstStart > secondStart){
			if (firstEnd < secondEnd) relation = IA_Relation.di;
			if (firstEnd == secondEnd) relation = IA_Relation.f;
			if (firstEnd > secondEnd) relation = IA_Relation.oi;			
		}
		
		assert (relation != 0);
	}
}
