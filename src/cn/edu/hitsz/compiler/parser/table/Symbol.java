package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol {
    private String name;  // 符号名
    private Term symbol;  // 数据类型
    private SourceCodeType type = null;  // 属性
    private int value = 0;  // 数值

    public Symbol(Term symbol) {     // 纯终结符或非终结符的构造方法
        this.name = symbol.toString();
        this.symbol = symbol;
    }
    public Symbol(Term symbol,SourceCodeType type) {  // 标识符属性终结符的构造方法
        this.name = symbol.toString();
        this.symbol = symbol;
        this.type = type;
    }
    public Symbol(Term symbol,int value) {   // 数值终结符的构造方法
        this.name = symbol.toString();
        this.symbol = symbol;
        this.value = value;
    }
    public Symbol(Term symbol,String name) {   // 标识符终结符的构造方法
        this.name = name;
        this.symbol = symbol;
    }

    public String getName(){
        return name;
    }
    public Term getSymbol() {
        return symbol;
    }
    public SourceCodeType getType() {
        return type;
    }
    public int getValue() {
        return value;
    }
    public void setName(String newName) {
        name = newName;
    }
    public void setSymbol(Term symbol) {
        this.symbol = symbol;
    }
    public void setType(SourceCodeType type) {
        this.type = type;
    }
    public void setValue(int value) {
        this.value = value;
    }
    public void setSame(Symbol same) {   // 继承属性设置
        this.type = same.getType();
        this.value = same.getValue();
    }

}
