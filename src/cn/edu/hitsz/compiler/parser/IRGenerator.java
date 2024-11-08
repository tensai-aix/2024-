package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
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
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private final List<Instruction> instructions = new ArrayList<>();
    private SymbolTable symbolTable;
    private final Stack<Symbol> symbolStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        TokenKind kind = currentToken.getKind();
        switch (kind.getIdentifier()){
            case "IntConst"->{
                symbolStack.add(new Symbol(kind, Integer.parseInt(currentToken.getText())));
            }
            case "id"->{
                Symbol symbol = new Symbol(kind, currentToken.getText());
                symbol.setValue(symbolTable.getValue(currentToken.getText()));
                symbolStack.add(symbol);
            }
            default -> {
                symbolStack.add(new Symbol(kind));
            }
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
                instructions.add(Instruction.createMov(new IRVariable(tmp_symbols.get(2).getName()),new IRImmediate(tmp_symbols.getFirst().getValue())));
                tmp_symbols.get(2).setValue(tmp_symbols.get(0).getValue());
                symbolTable.updateValue(tmp_symbols.get(2).getName(), tmp_symbols.get(2).getValue());
                break;
            }
            case 7: {   // S -> return E;
                instructions.add(Instruction.createRet(new IRImmediate(tmp_symbols.getFirst().getValue())));
                break;
            }
            case 8: {   // E -> E + A;
                symbol.setValue((tmp_symbols.getFirst().getValue()) + (tmp_symbols.get(2).getValue()));
                instructions.add(Instruction.createAdd(new IRVariable(symbol.getName()),new IRImmediate((tmp_symbols.get(2).getValue())),new IRImmediate((tmp_symbols.get(0).getValue()))));
                break;
            }
            case 9: {   // E -> E - A;
                symbol.setValue((tmp_symbols.get(2).getValue()) - (tmp_symbols.get(0).getValue()));
                instructions.add(Instruction.createSub(new IRVariable(symbol.getName()),new IRImmediate((tmp_symbols.get(2).getValue())),new IRImmediate((tmp_symbols.get(0).getValue()))));
                break;
            }
            case 11: {  // A -> A * B;
                symbol.setValue((tmp_symbols.getFirst().getValue()) * (tmp_symbols.get(2).getValue()));
                instructions.add(Instruction.createMul(new IRVariable(symbol.getName()),new IRImmediate((tmp_symbols.get(2).getValue())),new IRImmediate((tmp_symbols.get(0).getValue()))));
                break;
            }
            case 13: {  // B -> ( E );
                symbol.setValue(tmp_symbols.get(1).getValue());
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
        symbolTable = table;
    }

    public List<Instruction> getIR() {
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }

}

