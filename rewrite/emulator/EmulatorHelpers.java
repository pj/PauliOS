package emulator;

import machine.Lib;

public class EmulatorHelpers {
	
	public static boolean test(int flag, Processor processor) {
		return Lib.test(flag, processor.flags);
	}
	
	public static void print(Processor processor) {
		if (Lib.test(processor.dbgDisassemble) && Lib.test(processor.dbgProcessor)
				&& !Lib.test(processor.dbgFullDisassemble))
			System.out.print("PC=0x" + Lib.toHexString(processor.registers[processor.regPC])
					+ "\t");

		if (processor.operation == Mips.INVALID) {
			System.out.print("invalid: op=" + Lib.toHexString(processor.op, 2)
					+ " rs=" + Lib.toHexString(processor.rs, 2) + " rt="
					+ Lib.toHexString(processor.rt, 2) + " rd="
					+ Lib.toHexString(processor.rd, 2) + " sh="
					+ Lib.toHexString(processor.sh, 2) + " func="
					+ Lib.toHexString(processor.func, 2) + "\n");
			return;
		}

		int spaceIndex = processor.name.indexOf(' ');
		Lib
				.assert_(spaceIndex != -1
						&& spaceIndex == processor.name.lastIndexOf(' '));

		String instname = processor.name.substring(0, spaceIndex);
		char[] args = processor.name.substring(spaceIndex + 1).toCharArray();

		System.out.print(instname + "\t");

		int minCharsPrinted = 0, maxCharsPrinted = 0;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case Mips.RS:
				System.out.print("$" + processor.rs);
				minCharsPrinted += 2;
				maxCharsPrinted += 3;

				if (Lib.test(processor.dbgFullDisassemble)) {
					System.out
							.print("#0x" + Lib.toHexString(processor.registers[processor.rs]));
					minCharsPrinted += 11;
					maxCharsPrinted += 11;
				}
				break;
			case Mips.RT:
				System.out.print("$" + processor.rt);
				minCharsPrinted += 2;
				maxCharsPrinted += 3;

				if (Lib.test(processor.dbgFullDisassemble)
						&& (i != 0 || !test(Mips.DST, processor))
						&& !test(Mips.DELAYEDLOAD, processor)) {
					System.out
							.print("#0x" + Lib.toHexString(processor.registers[processor.rt]));
					minCharsPrinted += 11;
					maxCharsPrinted += 11;
				}
				break;
			case Mips.RETURNADDRESS:
				if (processor.rd == 31)
					continue;
			case Mips.RD:
				System.out.print("$" + processor.rd);
				minCharsPrinted += 2;
				maxCharsPrinted += 3;
				break;
			case Mips.IMM:
				System.out.print(processor.imm);
				minCharsPrinted += 1;
				maxCharsPrinted += 6;
				break;
			case Mips.SHIFTAMOUNT:
				System.out.print(processor.sh);
				minCharsPrinted += 1;
				maxCharsPrinted += 2;
				break;
			case Mips.ADDR:
				System.out.print(processor.imm + "($" + processor.rs);
				minCharsPrinted += 4;
				maxCharsPrinted += 5;

				if (Lib.test(processor.dbgFullDisassemble)) {
					System.out
							.print("#0x" + Lib.toHexString(processor.registers[processor.rs]));
					minCharsPrinted += 11;
					maxCharsPrinted += 11;
				}

				System.out.print(")");
				break;
			case Mips.TARGET:
				System.out.print("0x" + Lib.toHexString(processor.jtarget));
				minCharsPrinted += 10;
				maxCharsPrinted += 10;
				break;
			default:
				Lib.assert_(false);
			}
			if (i + 1 < args.length) {
				System.out.print(", ");
				minCharsPrinted += 2;
				maxCharsPrinted += 2;
			} else {
				// most separation possible is tsi, 5+1+1=7,
				// thankfully less than 8 (makes this possible)
				Lib.assert_(maxCharsPrinted - minCharsPrinted < 8);
				// longest string is stj, which is 40-42 chars w/ -d M;
				// go for 48
				while ((minCharsPrinted % 8) != 0) {
					System.out.print(" ");
					minCharsPrinted++;
					maxCharsPrinted++;
				}
				while (minCharsPrinted < 48) {
					System.out.print("\t");
					minCharsPrinted += 8;
				}
			}
		}

		if (Lib.test(processor.dbgDisassemble) && Lib.test(processor.dbgProcessor)
				&& !Lib.test(processor.dbgFullDisassemble))
			System.out.print("\n");
	}
}
