package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;


public class VariableStatement extends Statement {
    private Expression expression;
    private String variableName;
    private CatscriptType explicitType;
    private CatscriptType type;

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public void setExplicitType(CatscriptType type) {
        this.explicitType = type;
    }

    public CatscriptType getExplicitType() {
        return explicitType;
    }

    public boolean isGlobal() {
        return getParent() instanceof CatScriptProgram;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {

            type = null;
            if(explicitType != null){
                type = explicitType;
                if(!explicitType.isAssignableFrom(expression.getType())){
                    addError(ErrorType.INCOMPATIBLE_TYPES);
                }
            } else {
                type = expression.getType();
            }
            symbolTable.registerSymbol(variableName, type);
        }
    }

    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        runtime.setValue(variableName, expression.evaluate(runtime));
    }

    @Override
    public void transpile(StringBuilder javascript) {
        javascript.append("var").append(variableName).append("=");
        expression.transpile(javascript);
        javascript.append(";");
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        if(isGlobal()){
            if(getType().equals(CatscriptType.INT) || getType().equals(CatscriptType.BOOLEAN)) {
                code.addField(variableName, "I");
                code.addVarInstruction(Opcodes.ALOAD, 0);
                expression.compile(code);
                code.addFieldInstruction(Opcodes.PUTFIELD, variableName, "I", code.getProgramInternalName());
            }
            else {
                code.addField(variableName, "L" + ByteCodeGenerator.internalNameFor(getType().getJavaType()) + ";");
                code.addVarInstruction(Opcodes.ALOAD, 0);
                expression.compile(code);
                code.addFieldInstruction(Opcodes.PUTFIELD, variableName, "L" + ByteCodeGenerator.internalNameFor(getType().getJavaType()) + ";", code.getProgramInternalName());
            }
        }
        else {
            Integer localSlot = code.createLocalStorageSlotFor(variableName);
            if(getType().equals(CatscriptType.INT) || getType().equals(CatscriptType.BOOLEAN)) {
                expression.compile(code);
                code.addVarInstruction(Opcodes.ISTORE, localSlot);
            }
            else {
                expression.compile(code);
                code.addVarInstruction(Opcodes.LSTORE, localSlot);
            }
        }
    }
}
