package edu.cmu.cs.mvelezce.soot.jimple.jtp;

import soot.*;
import soot.jimple.*;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlFlowSink extends BodyTransformer {
    private static ControlFlowSink instance = new ControlFlowSink();

    private ControlFlowSink() {
        ;
    }

    public static ControlFlowSink v() {
        return ControlFlowSink.instance;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        Chain<Unit> units = b.getUnits();
        Chain<Local> locals = b.getLocals();
        Map<Unit, List<Value>> decisionsToLocals = this.getDecisionsToLocals(units, locals);

        if(decisionsToLocals.isEmpty()) {
            return;
        }

        SootClass sootClass = Scene.v().loadClassAndSupport("edu.cmu.cs.mvelezce.analysis.option.Sink");
        SootMethod sootMethod = sootClass.getMethod("void sink(java.lang.Object)");

        this.addSinks(decisionsToLocals, units, sootMethod);
    }

    private Map<Unit, List<Value>> getDecisionsToLocals(Chain<Unit> units, Chain<Local> locals) {
        Map<Unit, List<Value>> decisionsToLocals = new HashMap<>();

        for(Unit unit : units) {
            if(unit instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) unit;
                Value cond = ifStmt.getCondition();

                if(cond instanceof ConditionExpr) {
                    ConditionExpr condExpr = (ConditionExpr) cond;
                    Value op1 = condExpr.getOp1();
                    Value op2 = condExpr.getOp2();

                    if(locals.contains(op1)) {
                       this.addLocalToDecision(decisionsToLocals, unit, op1);
                    }

                    if(locals.contains(op2)) {
                        this.addLocalToDecision(decisionsToLocals, unit, op2);
                    }
                }
                else {
                    throw new RuntimeException("Other type of condition");
                }
            }
            else if(unit instanceof SwitchStmt) {
                SwitchStmt switchStmt = (SwitchStmt) unit;
                Value cond = switchStmt.getKey();

                if(locals.contains(cond)) {
                    this.addLocalToDecision(decisionsToLocals, switchStmt, cond);
                }
            }
        }

        return decisionsToLocals;
    }

    private void addLocalToDecision(Map<Unit, List<Value>> decisionsToLocals, Unit unit, Value op) {
        if(!decisionsToLocals.containsKey(unit)) {
            List<Value> values = new ArrayList<>();
            decisionsToLocals.put(unit, values);
        }

        decisionsToLocals.get(unit).add(op);
    }

    private void addSinks(Map<Unit, List<Value>> decisionsToLocals, Chain<Unit> units, SootMethod sootMethod) {
        for(Map.Entry<Unit, List<Value>> unitToValues : decisionsToLocals.entrySet()) {
            for(Value value : unitToValues.getValue()) {
                StaticInvokeExpr invExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(), value);
                Stmt stmt = Jimple.v().newInvokeStmt(invExpr);

                Tag lineNumberTag = null;
                Tag bytecodeOffsetTag = null;

                for(Tag tag : unitToValues.getKey().getTags()) {
                    if(tag instanceof LineNumberTag) {
                        int javaLine = ((LineNumberTag) tag).getLineNumber();
                        lineNumberTag = new LineNumberTag(javaLine);
                    }

                    if(tag instanceof BytecodeOffsetTag) {
                        int bytecodeIndex = ((BytecodeOffsetTag) tag).getBytecodeOffset();
                        bytecodeOffsetTag = new BytecodeOffsetTag(bytecodeIndex);
                    }
                }

                if(lineNumberTag == null) {
                    lineNumberTag = new LineNumberTag(-1);
                }

                if(bytecodeOffsetTag == null) {
                    bytecodeOffsetTag = new BytecodeOffsetTag(-1);
                }

                stmt.addTag(lineNumberTag);
                stmt.addTag(bytecodeOffsetTag);

                units.insertBefore(stmt, unitToValues.getKey());

//                units.insertAfter(stmt, unitToValues.getKey());

//                NopStmt nop = Jimple.v().newNopStmt();
//                nop.addTag(lineNumberTag);
//                nop.addTag(bytecodeOffsetTag);
//                units.insertBefore(nop, stmt);
            }
        }

    }

}