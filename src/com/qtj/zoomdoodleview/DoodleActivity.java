package com.qtj.zoomdoodleview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class DoodleActivity extends Activity {

	private String rootPath = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/XxDrawing";
	private ZoomDoodleView doodleView = null;
	private Bitmap background = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_doodle);
		// background = BitmapFactory.decodeResource(this.getResources(),
		// R.drawable.background);
		doodleView = (ZoomDoodleView) findViewById(R.id.zdv);
		doodleView.setColor(Color.BLACK);
		doodleView.setBrushSize(10f);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_doodle, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_line_mode:
			// 自由曲线
			doodleView.setEnableDragAndZoom(false);
			doodleView.setMode(ZoomDoodleView.Mode.LINE_MODE);
			break;
		case R.id.menu_circle_mode:
			doodleView.setEnableDragAndZoom(false);
			doodleView.setMode(ZoomDoodleView.Mode.CIRCLE_MODE);
			break;
		case R.id.menu_eraser_mode:
			doodleView.setEnableDragAndZoom(false);
			doodleView.setMode(ZoomDoodleView.Mode.ERASER_MODE);
			break;
		case R.id.menu_rect_mode:
			doodleView.setEnableDragAndZoom(false);
			doodleView.setMode(ZoomDoodleView.Mode.RECT_MODE);
			break;
		case R.id.menu_roll_back:
			doodleView.setEnableDragAndZoom(false);
			doodleView.rollBack();
			break;
		case R.id.menu_clear_screen:
			// doodleView.setEnableDragAndZoom(false);
			doodleView.clear();
			break;
		case R.id.menu_enable_drag_and_zoom:
			doodleView.setEnableDragAndZoom(true);
			doodleView.setMode(ZoomDoodleView.Mode.NONE_MODE);
			break;
		case R.id.menu_deep_eraser_mode:
			doodleView.setEnableDragAndZoom(false);
			doodleView.setMode(ZoomDoodleView.Mode.DEEP_ERASER_MODE);
			break;
		case R.id.menu_text:
			showInputTextDialog();
			break;
		case R.id.menu_save:
			savePicAsync();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 显示输入文本对话框
	 */
	private void showInputTextDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("请输入文字");
		final EditText editText = new EditText(this);
		builder.setView(editText);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = editText.getText().toString();
				doodleView.setMode(ZoomDoodleView.Mode.TEXT_MODE);
				doodleView.addTextAction(text);
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		builder.show();
	}

	/**
	 * 保存当前内容 异步
	 */
	private void savePicAsync() {
		new Thread() {
			public void run() {
				if (doodleView.saveToFile(rootPath + "/qin-"
						+ System.currentTimeMillis() + ".png")) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(DoodleActivity.this, "保存成功！",
									Toast.LENGTH_SHORT).show();
						};
					});
				}
			}
		}.start();
	}

}
