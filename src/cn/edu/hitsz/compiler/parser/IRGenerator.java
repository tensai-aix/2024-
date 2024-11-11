package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private final List<Instruction> instructions = new ArrayList<>();
    private final Stack<Symbol> symbolStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        TokenKind kind = currentToken.getKind();
        if(Objects.equals(kind.getIdentifier(), "IntConst") || Objects.equals(kind.getIdentifier(), "id")){
            symbolStack.add(new Symbol(kind, currentToken.getText()));
        }
        else{
            symbolStack.add(new Symbol(kind));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        List<Symbol> tmp_symbols = new ArrayList<>();
        Symbol symbol = new Symbol(production.head());
        for(int tmp=0; tmp<production.body().size(); tmp++){
            tmp_symbols.add(symbolStack.pop());
        }
        symbol.setSame(tmp_symbols.getFirst());
        switch (production.index()){   // 只用讨论涉及了值和名字的或中间代码生成的、body含有多个元素的production
            case 6: {   // S -> id = E;
                instructions.add(Instruction.createMov(new IRVariable(tmp_symbols.get(2).getName()),getIRValue(tmp_symbols.getFirst())));
                break;
            }
            case 7: {   // S -> return E;
                instructions.add(Instruction.createRet(getIRValue(tmp_symbols.getFirst())));
                break;
            }
            case 8: {   // E -> E + A;
                symbol.setName(IRVariable.temp().getName());
                instructions.add(Instruction.createAdd(new IRVariable(symbol.getName()),getIRValue(tmp_symbols.get(2)),getIRValue(tmp_symbols.get(0))));
                break;
            }
            case 9: {   // E -> E - A;
                symbol.setName(IRVariable.temp().getName());
                instructions.add(Instruction.createSub(new IRVariable(symbol.getName()),getIRValue(tmp_symbols.get(2)),getIRValue(tmp_symbols.get(0))));
                break;
            }
            case 11: {  // A -> A * B;
                symbol.setName(IRVariable.temp().getName());
                instructions.add(Instruction.createMul(new IRVariable(symbol.getName()),getIRValue(tmp_symbols.get(2)),getIRValue(tmp_symbols.get(0))));
                break;
            }
            case 13: {  // B -> ( E );
                symbol.setName(tmp_symbols.get(1).getName());
                break;
            }
        }
        symbolStack.push(symbol);
    }


    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
    }

    public List<Instruction> getIR() {
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }

    public IRValue getIRValue(Symbol symbol){    // 获取symbol对应的IRValue
        if(symbol.getName().matches("\\d+")){   // 如果是数字，构造IRImmediate
            return new IRImmediate(Integer.parseInt(symbol.getName()));
        }
        else{   // 否则构造IRVariable
            return new IRVariable(symbol.getName());
        }
    }

}