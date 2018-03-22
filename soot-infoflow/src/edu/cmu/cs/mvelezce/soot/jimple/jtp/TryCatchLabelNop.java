package edu.cmu.cs.mvelezce.soot.jimple.jtp;

import soot.Body;
import soot.BodyTransformer;
import soot.Trap;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.util.Chain;

import java.util.Map;

public class TryCatchLabelNop extends BodyTransformer {
    private static TryCatchLabelNop instance = new TryCatchLabelNop();

    private TryCatchLabelNop() {
        ;
    }

    public static TryCatchLabelNop v() {
        return TryCatchLabelNop.instance;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        Chain<Unit> units = b.getUnits();
        Chain<Trap> traps = b.getTraps();

        if(traps.isEmpty()) {
            return;
        }

        for(Trap trap : traps) {
            Unit beginUnit = trap.getBeginUnit();

            // Contains method does not work. Have to loop :(
            for(Unit unit : units) {
                if(unit.equals(beginUnit)) {
                    NopStmt nop = Jimple.v().newNopStmt();
                    units.insertBefore(nop, beginUnit);

                    break;
                }
            }
        }

    }

}