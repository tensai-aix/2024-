package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.*;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;
    private final Stack<Symbol> symbolStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        List<Symbol> tmp_symbols = new ArrayList<>();
        Symbol symbol = new Symbol(production.head());
        int index = production.index();
        for (int tmp = 0; tmp < production.body().size(); tmp++) {
            tmp_symbols.add(symbolStack.pop());
        }
        symbol.setSame(tmp_symbols.get(0));
        if (index == 4) {   // S -> D id;只用讨论涉及了属性的、body含有多个元素的production
            tmp_symbols.get(0).setType(tmp_symbols.get(1).getType());
            symbolTable.updateType(tmp_symbols.getFirst().getName(), tmp_symbols.getFirst().getType());  // 更新符号表
        }
        symbolStack.push(symbol);
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        TokenKind kind = currentToken.getKind();
        switch (kind.getIdentifier()){
            case "int"->{
                symbolStack.add(new Symbol(kind, SourceCodeType.Int));
            }
            case "id"->{
                symbolStack.add(new Symbol(kind, currentToken.getText()));
            }
            default -> {
                symbolStack.add(new Symbol(kind));
            }
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }
}

