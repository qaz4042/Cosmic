public class CheckPetFilter {
 public static void main(String[] args) {
  for(int mask=0;mask<4;mask++) {
   if(server.TrainingPetVacuum.accepts(mask,true)!=((mask&1)!=0)) throw new AssertionError();
   if(server.TrainingPetVacuum.accepts(mask,false)!=((mask&2)!=0)) throw new AssertionError();
  }
  System.out.println("PASS: all four server loot filter combinations");
 }
}
