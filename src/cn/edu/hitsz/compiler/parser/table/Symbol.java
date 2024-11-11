package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol {
    private String name;  // 符号名
    private final Term symbol;  // 数据类型
    private SourceCodeType type = null;  // 属性

    public Symbol(Term symbol) {     // 纯终结符或非终结符的构造方法
        this.name = symbol.toString();
        this.symbol = symbol;
    }
    public Symbol(Term symbol,SourceCodeType type) {  // 标识符属性终结符的构造方法
        this.name = symbol.toString();
        this.symbol = symbol;
        this.type = type;
    }
    public Symbol(Term symbol,String name) {   // 标识符或数值符终结符的构造方法
        this.name = name;
        this.symbol = symbol;
    }

    public String getName(){
        return name;
    }
    public SourceCodeType getType() {
        return type;
    }
    public void setName(String newName) {
        name = newName;
    }
    public void setType(SourceCodeType type) {
        this.type = type;
    }
    public void setSame(Symbol same) {   // 继承属性设置
        this.name = same.getName();
        this.type = same.getType();
    }

}
