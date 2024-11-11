package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.parser.table.BMap;
import cn.edu.hitsz.compiler.parser.table.Reg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private final List<Reg> free_regs = new ArrayList<>();  // 空闲寄存器列表
    private final List<Reg> used_regs = new ArrayList<>();  // 使用的寄存器列表
    private final BMap<Reg,IRVariable> RI = new BMap<>();  // 寄存器和变量的双映射表
    private final StringBuilder stringBuilder = new StringBuilder();  // 生成汇编代码
    private final List<Instruction> instructions = new ArrayList<>();  // 更新后的中间代码
    private final Map<IRVariable,Integer> memory = new HashMap<>();  // 模拟内存
    private final Map<IRVariable,Integer> appear_time = new HashMap<>();  // 每个变量的使用次数
    private final Reg sp = new Reg("sp");  // sp寄存器
    private final Reg a0 = new Reg("a0");  // a0寄存器

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // 预处理
        for (Instruction instruction : originInstructions) {
            if (instruction.getKind() == InstructionKind.ADD || instruction.getKind() == InstructionKind.SUB || instruction.getKind() == InstructionKind.MUL) {
                if (instruction.getLHS() instanceof IRImmediate && instruction.getRHS() instanceof IRImmediate) {
                    switch (instruction.getKind()) {
                        case ADD:
                            instructions.add(Instruction.createMov(instruction.getResult(), new IRImmediate(((IRImmediate) instruction.getLHS()).getValue() + ((IRImmediate) instruction.getRHS()).getValue())));
                        case SUB:
                            instructions.add(Instruction.createMov(instruction.getResult(), new IRImmediate(((IRImmediate) instruction.getLHS()).getValue() - ((IRImmediate) instruction.getRHS()).getValue())));
                        case MUL:
                            instructions.add(Instruction.createMov(instruction.getResult(), new IRImmediate(((IRImmediate) instruction.getLHS()).getValue() * ((IRImmediate) instruction.getRHS()).getValue())));
                    }
                } else if ((instruction.getKind() == InstructionKind.ADD && instruction.getRHS() instanceof IRVariable && instruction.getLHS() instanceof IRImmediate)) {
                    instructions.add(Instruction.createAdd(instruction.getResult(), instruction.getRHS(), instruction.getLHS()));
                    addAppearTime(instruction.getRHS());
                    addAppearTime(instruction.getLHS());
                } else if (instruction.getKind() == InstructionKind.MUL && instruction.getLHS() instanceof IRImmediate) {
                    IRVariable tmp_IRValue = new IRVariable(IRVariable.temp().getName());
                    instructions.add(Instruction.createMov(tmp_IRValue, instruction.getLHS()));
                    addAppearTime(tmp_IRValue);
                    instructions.add(Instruction.createMul(instruction.getResult(), tmp_IRValue, instruction.getRHS()));
                    addAppearTime(instruction.getRHS());
                    addAppearTime(tmp_IRValue);
                } else if (instruction.getKind() == InstructionKind.MUL && instruction.getRHS() instanceof IRImmediate) {
                    IRVariable tmp_IRValue = new IRVariable(IRVariable.temp().getName());
                    instructions.add(Instruction.createMov(tmp_IRValue, instruction.getRHS()));
                    addAppearTime(tmp_IRValue);
                    instructions.add(Instruction.createMul(instruction.getResult(), instruction.getLHS(), tmp_IRValue));
                    addAppearTime(instruction.getLHS());
                    addAppearTime(tmp_IRValue);
                } else if (instruction.getKind() == InstructionKind.SUB && instruction.getLHS() instanceof IRImmediate) {
                    IRVariable tmp_IRValue = new IRVariable(IRVariable.temp().getName());
                    instructions.add(Instruction.createMov(tmp_IRValue, instruction.getLHS()));
                    addAppearTime(tmp_IRValue);
                    instructions.add(Instruction.createSub(instruction.getResult(), tmp_IRValue, instruction.getRHS()));
                    addAppearTime(instruction.getRHS());
                    addAppearTime(tmp_IRValue);
                } else{
                    instructions.add(instruction);
                    addAppearTime(instruction.getLHS());
                    addAppearTime(instruction.getRHS());
                }
                addAppearTime(instruction.getResult());
            }
            else if(instruction.getKind() == InstructionKind.MOV){
                instructions.add(instruction);
                addAppearTime(instruction.getFrom());
                addAppearTime(instruction.getResult());
            }
            else if(instruction.getKind() == InstructionKind.RET){
                instructions.add(instruction);
                addAppearTime(instruction.getReturnValue());
            }
        }

        // 初始化寄存器
        int numberOf_variable_regs = 7;   // 可用的寄存器数
        for (int i = 0; i < numberOf_variable_regs; i++) {
            free_regs.add(new Reg("t" + i));
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        stringBuilder.append(".text\n");
        for (Instruction instruction : instructions) {
            if (instruction.getKind() == InstructionKind.MOV) {
                Reg result_reg = findReg(instruction.getResult());
                if(instruction.getFrom() instanceof IRImmediate){
                    stringBuilder.append("    li ").append(result_reg).append(", ").append(((IRImmediate) instruction.getFrom()).getValue()).append("\n");
                }
                else if(instruction.getFrom() instanceof IRVariable){
                    Reg from_reg = findReg((IRVariable) instruction.getFrom());
                    stringBuilder.append("    mv ").append(result_reg).append(", ").append(from_reg).append("\n");
                }
                decreaseAppearTime(instruction.getFrom());
                decreaseAppearTime(instruction.getResult());
            }
            else if (instruction.getKind() == InstructionKind.ADD) {
                Reg result_reg = findReg(instruction.getResult());
                Reg left_reg = findReg((IRVariable) instruction.getLHS());
                if(instruction.getRHS() instanceof IRImmediate){
                    stringBuilder.append("    addi ").append(result_reg).append(", ").append(left_reg).append(", ").append(((IRImmediate) instruction.getRHS()).getValue()).append("\n");
                }
                else if(instruction.getRHS() instanceof IRVariable){
                    Reg right_reg = findReg((IRVariable) instruction.getRHS());
                    stringBuilder.append("    add ").append(result_reg).append(", ").append(left_reg).append(", ").append(right_reg).append("\n");
                }
                decreaseAppearTime(instruction.getRHS());
                decreaseAppearTime(instruction.getLHS());
                decreaseAppearTime(instruction.getResult());
            }
            else if (instruction.getKind() == InstructionKind.SUB) {
                Reg result_reg = findReg(instruction.getResult());
                Reg left_reg = findReg((IRVariable) instruction.getLHS());
                Reg right_reg = findReg((IRVariable) instruction.getRHS());
                stringBuilder.append("    sub ").append(result_reg).append(", ").append(left_reg).append(", ").append(right_reg).append("\n");
                decreaseAppearTime(instruction.getRHS());
                decreaseAppearTime(instruction.getLHS());
                decreaseAppearTime(instruction.getResult());
            }
            else if (instruction.getKind() == InstructionKind.MUL) {
                Reg result_reg = findReg(instruction.getResult());
                Reg left_reg = findReg((IRVariable) instruction.getLHS());
                Reg right_reg = findReg((IRVariable) instruction.getRHS());
                stringBuilder.append("    mul ").append(result_reg).append(", ").append(left_reg).append(", ").append(right_reg).append("\n");
                decreaseAppearTime(instruction.getRHS());
                decreaseAppearTime(instruction.getLHS());
                decreaseAppearTime(instruction.getResult());
            }
            else if (instruction.getKind() == InstructionKind.RET) {
                Reg return_reg = findReg((IRVariable) instruction.getReturnValue());
                if(!memory.isEmpty()){
                    stringBuilder.append("    addi ").append(sp).append(", ").append(sp).append(", ").append(4 * memory.size()).append("\n");
                }
                stringBuilder.append("    mv ").append(a0).append(", ").append(return_reg).append("\n");
                decreaseAppearTime(instruction.getReturnValue());
            }
        }
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            writer.write(stringBuilder.toString());
        } catch (IOException _) {
        }
    }

    /**
     * 寄存器许选择算法
     */
    public Reg findReg(IRVariable irVariable){
        if(!RI.containsValue(irVariable)){
            Reg reg;
            if(!free_regs.isEmpty()){
                reg = free_regs.getFirst();
                free_regs.removeFirst();
            }
            else{
                reg = used_regs.getFirst();
                IRVariable to_be_changed_IRVariable = RI.getByKey(reg);
                if(!memory.containsKey(to_be_changed_IRVariable)){
                    memory.put(to_be_changed_IRVariable, memory.size()+1);
                }
                stringBuilder.append("    addi ").append(sp).append(", ").append(sp).append(", ").append("-4").append("\n");
                stringBuilder.append("    sw ").append(reg).append(", ").append(0).append("(").append(sp).append(")").append("\n");
                used_regs.removeFirst();
            }
            if(memory.containsKey(irVariable)){
                stringBuilder.append("    lw ").append(reg).append(", ").append(4 * (memory.size() - memory.get(irVariable))).append("(").append(sp).append(")").append("\n");
            }
            used_regs.add(reg);
            RI.put(reg,irVariable);
            return reg;
        }
        else{
            return RI.getByValue(irVariable);
        }
    }

    /**
     * 增加变量的出现次数
     */
    public void addAppearTime(IRValue irVariable){
        if(irVariable instanceof IRVariable){
            if(!appear_time.containsKey(irVariable)){
                appear_time.put((IRVariable) irVariable,1);
            }
            else{
                appear_time.put((IRVariable) irVariable,appear_time.get(irVariable)+1);
            }
        }
    }

    /**
     * 减少变量的出现次数
     */
    public void decreaseAppearTime(IRValue irVariable){
        if(irVariable instanceof IRVariable){
            int left_time = appear_time.get(irVariable) - 1;
            if(left_time == 0){
                appear_time.remove(irVariable);
                Reg to_free_reg = RI.getByValue((IRVariable) irVariable);
                RI.removeByValue((IRVariable) irVariable);
                used_regs.remove(to_free_reg);
                free_regs.add(to_free_reg);
            }
            else{
                appear_time.put((IRVariable) irVariable,left_time);
            }
        }
    }
}

