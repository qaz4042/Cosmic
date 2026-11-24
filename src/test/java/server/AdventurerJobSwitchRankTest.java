package server;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AdventurerJobSwitchRankTest {
 @Test void everyBranchPreservesActualRank() {
  int[][] groups={{110,120,130},{210,220,230},{310,320},{410,420},{510,520}};
  for(int[] group:groups) for(int source:group) for(int rank=0;rank<3;rank++) for(int target:group) {
   assertEquals(target+rank,AdventurerJobSwitch.targetJob(source+rank,target));
   assertTrue(AdventurerJobSwitch.sameFamily(source+rank,target+rank));
   assertFalse(AdventurerJobSwitch.sameFamily(source+rank,target+(rank+1)%3));
  }
  assertEquals(-1,AdventurerJobSwitch.targetJob(222,510));
  assertEquals(-1,AdventurerJobSwitch.targetJob(222,232));
  assertFalse(AdventurerJobSwitch.branch(900));
 }
 @Test void refundsAllAncestorBranchSkillsButKeepsFirstJob() {
  assertEquals(67,AdventurerJobSwitch.refundedSp(222,7,Map.of(2001002,20,2201001,20,2211002,30,2221006,10)));
  assertEquals(67,AdventurerJobSwitch.refundedSp(232,67,Map.of(2001002,20,2201001,0,2221006,0)));
  assertThrows(IllegalArgumentException.class,()->AdventurerJobSwitch.refundedSp(221,0,Map.of(2221006,1)));
  assertThrows(IllegalArgumentException.class,()->AdventurerJobSwitch.refundedSp(222,0,Map.of(2321006,1)));
 }
}
