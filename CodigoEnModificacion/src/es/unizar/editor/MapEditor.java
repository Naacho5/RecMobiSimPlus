package es.unizar.editor;

import java.math.RoundingMode;

import es.unizar.editor.controller.MapEditorController;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.view.MapEditorView;
import es.unizar.spatialDB.DatabaseAccess;
import es.unizar.util.EditorLiterals;

public class MapEditor {
	
	public DatabaseAccess db;
		
	public MapEditor(DatabaseAccess db, String mapToEdit) {
		super();
		
		EditorLiterals.decimalFormat.setRoundingMode(RoundingMode.HALF_UP); // Set rounding mode -> approximation
		final MapEditorModel model = new MapEditorModel();
		final MapEditorController controller = new MapEditorController(model);
		final MapEditorView view = new MapEditorView(model, controller, db, mapToEdit);
		controller.setView(view);
	}
	
	public static void main(String[] args) {
		new MapEditor(new DatabaseAccess(),null);
	}
	
}
