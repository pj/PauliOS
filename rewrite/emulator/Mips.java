package emulator;

public class Mips {
	Mips() {
	}

	Mips(int operation, String name) {
		this.operation = operation;
		this.name = name;
	}

	Mips(int operation, String name, int format, int flags) {
		this(operation, name);
		this.format = format;
		this.flags = flags;
	}

	int operation = INVALID;
	String name = "invalid ";
	int format;
	int flags;

	// operation types
	static final int INVALID = 0, UNIMPL = 1, ADD = 2, SUB = 3, MULT = 4,
			DIV = 5, SLL = 6, SRA = 7, SRL = 8, SLT = 9, AND = 10, OR = 11,
			NOR = 12, XOR = 13, LUI = 14, MFLO = 21, MFHI = 22, MTLO = 23,
			MTHI = 24, JUMP = 25, BEQ = 26, BNE = 27, BLEZ = 28, BGTZ = 29,
			BLTZ = 30, BGEZ = 31, SYSCALL = 32, LOAD = 33, LWL = 36,
			LWR = 37, STORE = 38, SWL = 39, SWR = 40, MAX = 40;

	static final int IFMT = 1, JFMT = 2, RFMT = 3;

	static final int DST = 0x00000001, DSTRA = 0x00000002,
			OVERFLOW = 0x00000004, SRC1SH = 0x00000008,
			SRC2IMM = 0x00000010, UNSIGNED = 0x00000020, LINK = 0x00000040,
			DELAYEDLOAD = 0x00000080, SIZEB = 0x00000100,
			SIZEH = 0x00000200, SIZEW = 0x00000400, BRANCH = 0x00000800;

	static final char RS = 's', RT = 't', RD = 'd', IMM = 'i',
			SHIFTAMOUNT = 'h', ADDR = 'a', // imm(rs)
			TARGET = 'j', RETURNADDRESS = 'r'; // rd, or none if rd=31;
	// can't be last

	static final Mips[] optable = {
			new Mips(), // special
			new Mips(), // reg-imm
			new Mips(JUMP, "j j", JFMT, BRANCH),
			new Mips(JUMP, "jal j", JFMT, BRANCH | LINK | DST | DSTRA),
			new Mips(BEQ, "beq stj", IFMT, BRANCH),
			new Mips(BNE, "bne stj", IFMT, BRANCH),
			new Mips(BLEZ, "blez sj", IFMT, BRANCH),
			new Mips(BGTZ, "bgtz sj", IFMT, BRANCH),
			new Mips(ADD, "addi tsi", IFMT, DST | SRC2IMM | OVERFLOW),
			new Mips(ADD, "addiu tsi", IFMT, DST | SRC2IMM),
			new Mips(SLT, "slti tsi", IFMT, DST | SRC2IMM),
			new Mips(SLT, "sltiu tsi", IFMT, DST | SRC2IMM | UNSIGNED),
			new Mips(AND, "andi tsi", IFMT, DST | SRC2IMM),
			new Mips(OR, "ori tsi", IFMT, DST | SRC2IMM),
			new Mips(XOR, "xori tsi", IFMT, DST | SRC2IMM),
			new Mips(LUI, "lui ti", IFMT, DST | SRC2IMM | UNSIGNED),
			new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(BEQ, "beql stj", IFMT, BRANCH),
			new Mips(BNE, "bnel stj", IFMT, BRANCH),
			new Mips(BLEZ, "blezl sj", IFMT, BRANCH),
			new Mips(BGTZ, "bgtzl sj", IFMT, BRANCH), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(),
			new Mips(LOAD, "lb ta", IFMT, DELAYEDLOAD | SIZEB),
			new Mips(LOAD, "lh ta", IFMT, DELAYEDLOAD | SIZEH),
			new Mips(LWL, "lwl ta", IFMT, DELAYEDLOAD),
			new Mips(LOAD, "lw ta", IFMT, DELAYEDLOAD | SIZEW),
			new Mips(LOAD, "lbu ta", IFMT, DELAYEDLOAD | SIZEB | UNSIGNED),
			new Mips(LOAD, "lhu ta", IFMT, DELAYEDLOAD | SIZEH | UNSIGNED),
			new Mips(LWR, "lwr ta", IFMT, DELAYEDLOAD), new Mips(),
			new Mips(STORE, "sb ta", IFMT, SIZEB),
			new Mips(STORE, "sh ta", IFMT, SIZEH),
			new Mips(SWL, "swl ta", IFMT, 0),
			new Mips(STORE, "sw ta", IFMT, SIZEW), new Mips(), new Mips(),
			new Mips(SWR, "swr ta", IFMT, 0), new Mips(),
			new Mips(UNIMPL, "ll "), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(UNIMPL, "sc "), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), };

	static final Mips[] specialtable = {
			new Mips(SLL, "sll dth", RFMT, DST | SRC1SH), new Mips(),
			new Mips(SRL, "srl dth", RFMT, DST | SRC1SH),
			new Mips(SRA, "sra dth", RFMT, DST | SRC1SH),
			new Mips(SLL, "sllv dts", RFMT, DST), new Mips(),
			new Mips(SRL, "srlv dts", RFMT, DST),
			new Mips(SRA, "srav dts", RFMT, DST),
			new Mips(JUMP, "jr s", RFMT, BRANCH),
			new Mips(JUMP, "jalr rs", RFMT, BRANCH | LINK | DST),
			new Mips(), new Mips(), new Mips(SYSCALL, "syscall "),
			new Mips(UNIMPL, "break "), new Mips(),
			new Mips(UNIMPL, "sync "), new Mips(MFHI, "mfhi d", RFMT, DST),
			new Mips(MTHI, "mthi s", RFMT, 0),
			new Mips(MFLO, "mflo d", RFMT, DST),
			new Mips(MTLO, "mtlo s", RFMT, 0), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(MULT, "mult st", RFMT, 0),
			new Mips(MULT, "multu st", RFMT, UNSIGNED),
			new Mips(DIV, "div st", RFMT, 0),
			new Mips(DIV, "divu st", RFMT, UNSIGNED), new Mips(),
			new Mips(), new Mips(), new Mips(),
			new Mips(ADD, "add dst", RFMT, DST | OVERFLOW),
			new Mips(ADD, "addu dst", RFMT, DST),
			new Mips(SUB, "sub dst", RFMT, DST | OVERFLOW),
			new Mips(SUB, "subu dst", RFMT, DST),
			new Mips(AND, "and dst", RFMT, DST),
			new Mips(OR, "or dst", RFMT, DST),
			new Mips(XOR, "xor dst", RFMT, DST),
			new Mips(NOR, "nor dst", RFMT, DST), new Mips(), new Mips(),
			new Mips(SLT, "slt dst", RFMT, DST),
			new Mips(SLT, "sltu dst", RFMT, DST | UNSIGNED), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), };

	static final Mips[] regimmtable = {
			new Mips(BLTZ, "bltz sj", IFMT, BRANCH),
			new Mips(BGEZ, "bgez sj", IFMT, BRANCH),
			new Mips(BLTZ, "bltzl sj", IFMT, BRANCH),
			new Mips(BGEZ, "bgezl sj", IFMT, BRANCH),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(),
			new Mips(BLTZ, "bltzal sj", IFMT, BRANCH | LINK | DST | DSTRA),
			new Mips(BGEZ, "bgezal sj", IFMT, BRANCH | LINK | DST | DSTRA),
			new Mips(BLTZ, "bltzlal sj", IFMT, BRANCH | LINK | DST | DSTRA),
			new Mips(BGEZ, "bgezlal sj", IFMT, BRANCH | LINK | DST | DSTRA),
			new Mips(), new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips(), new Mips(), new Mips(), new Mips(),
			new Mips(), new Mips() };
}