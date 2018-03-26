package cop5556fa17;

import java.util.HashMap;

import cop5556fa17.TypeUtils.Type;
import cop5556fa17.AST.Declaration;

class SymbolTable {
	private HashMap<String, Declaration> map;

	SymbolTable() {
		map = new HashMap<>();
	}

	public Type lookupType(String name) {
		return map.get(name).getType();
	}

	public void insert(String name, Declaration dec) {
		map.put(name, dec);
	}

	public Declaration lookupDec(String name) {
		return map.get(name);
	}

}
