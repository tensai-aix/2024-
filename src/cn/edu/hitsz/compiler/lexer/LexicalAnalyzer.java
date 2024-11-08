package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private static final int other = 0;
    private static final int simple_symbol = 1;
    private static final int complex_symbol = 2;
    private static final int number = 3;
    private static final int letter = 4;
    private static final int quotation = 5;

    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public String string;
    public List<Token> tokens = new ArrayList<>();
    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法,本次实验采用直接读取法
        StringBuilder content = new StringBuilder(); // 使用 StringBuilder 来构建字符串
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n"); // 逐行读取并附加换行符
            }
        } catch (IOException e) {
            e.printStackTrace(); // 打印异常信息
        }
        string = content.toString();
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        char c;
        char cnext;
        int index = 0;
        int nextIndex;
        while(index < string.length()) {
            nextIndex = index + 1;
            c = string.charAt(index);
            StringBuilder sb = new StringBuilder();
            switch (judgeType(c)) {
                case simple_symbol ->{                //单一符号
                    if(c == ';'){
                        tokens.add(Token.simple("Semicolon"));
                    }
                    else{
                        tokens.add(Token.simple(String.valueOf(c)));
                    }
                }
                case complex_symbol -> {              //复合符号
                    cnext = string.charAt(nextIndex);
                    if(cnext == c){
                        nextIndex++;
                        tokens.add(Token.simple(String.valueOf(c).repeat(2)));
                    }
                    else{
                        tokens.add(Token.simple(String.valueOf(c)));
                    }
                }
                case number -> {              //数字
                    sb.append(c);
                    while(true){
                        cnext = string.charAt(nextIndex);
                        if(judgeType(cnext) == number){
                            nextIndex++;
                            sb.append(cnext);
                        }
                        else{
                            break;
                        }
                    }
                    tokens.add(Token.normal("IntConst",sb.toString()));
                }
                case letter -> {              //字母
                    sb.append(c);
                    while(true){
                        cnext = string.charAt(nextIndex);
                        if(judgeType(cnext) == number || judgeType(cnext) == letter){
                            nextIndex++;
                            sb.append(cnext);
                        }
                        else{
                            break;
                        }
                    }
                    String result = sb.toString();
                    if (result.equals("int")){         //int
                        tokens.add(Token.simple("int"));
                    }
                    else if (result.equals("return")){          //return
                        tokens.add(Token.simple("return"));
                    }
                    else{
                        if(!(symbolTable.has(result))){
                            symbolTable.add(result);
                        }
                        tokens.add(Token.normal("id",result));
                    }
                }
                case quotation -> {
                    sb.append(c);
                    while(true){
                        cnext = string.charAt(nextIndex);
                        nextIndex++;
                        sb.append(cnext);
                        if(judgeType(cnext) == quotation){
                            break;
                        }
                    }
                    tokens.add(Token.normal("String",sb.toString()));
                }
            }
            index = nextIndex;
        }
        tokens.add(Token.simple("$"));
    }
    public int judgeType(char c){
        int type = other;  //空格换行符等等都包含在内
        if(c == ':' || c == '+' || c == '-' || c == '/' || c == '(' || c == ')' || c == ';'){
            type = simple_symbol;  //简单符号
        }
        else if(c == '*' || c == '='){
            type = complex_symbol;   //复合简单符号
        }
        else if(c >= '0' && c <= '9'){
            type = number;   //digit
        }
        else if(c >= 'a' && c <= 'z'){
            type = letter;   //leter
        }
        else if(c == '"'){
            type = quotation;   //引号
        }
        return type;
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
