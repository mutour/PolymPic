package com.knziha.polymer.pdviewer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.knziha.polymer.R;
import com.knziha.polymer.Toastable_Activity;
import com.knziha.polymer.Utils.CMN;
import com.knziha.polymer.Utils.Options;
import com.knziha.polymer.WeakReferenceHelper;
import com.knziha.polymer.databinding.ImageviewDebugBinding;
import com.knziha.polymer.widgets.AppIconsAdapter;

import static com.knziha.polymer.BrowserActivity.GoogleTranslate;

public class PDocViewerActivity extends Toastable_Activity {
	ImageviewDebugBinding UIData;
	private boolean hidingContextMenu;
	
	@Override
	public void onBackPressed() {
		if(UIData.wdv.draggingHandle==null && UIData.wdv.shouldDrawSelection()) {
			UIData.wdv.clearSelection();
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Window win = getWindow();
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		boolean transit = Options.getTransitSplashScreen();
		if(!transit) setTheme(R.style.AppThemeRaw);
		
		UIData = DataBindingUtil.setContentView(this, R.layout.imageview_debug);
		root=UIData.root;
		
		try {
			UIData.wdv.dm=dm;
			//PDocument pdoc = new PDocument(this, "/sdcard/myFolder/sample_hetero_dimension.pdf");
			//PDocument pdoc = new PDocument(this, "/sdcard/myFolder/Gpu Pro 1.pdf", dm, null);
			//UIData.wdv.setDocument(pdoc);
			
			UIData.wdv.a=this;
			
			
			UIData.wdv.setContextMenuView(UIData.contextMenu);
			
			if(transit){
				//root.setAlpha(0);
				closeSplashScreen();
			}
			
			UIData.wdv.setSelectionPaintView(UIData.sv);
			
			MenuBuilder context_menu = new MenuBuilder(this);
			getMenuInflater().inflate(R.menu.context_menu, context_menu);
			SpannableStringBuilder text = new SpannableStringBuilder();
			for (int i = 0; i < context_menu.size(); i++) {
				int start = text.length();
				MenuItem item = context_menu.getItem(i);
				text.append(item.getTitle());
				text.setSpan(new ClickableSpan() {
					@Override
					public void updateDrawState(TextPaint ds) {
					
					}
					@Override
					public void onClick(@NonNull View widget) {
						OnMenuClicked(item);
					}},start,text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				text.append("  ");
			}
			UIData.contextMenu.setText(text, TextView.BufferType.SPANNABLE);
			
			
			UIData.contextMenu.setOnClickListener(CMN.XYTouchRecorder());
			
			UIData.contextMenu.setOnTouchListener(CMN.XYTouchRecorder());
			
			
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/Gpu Pro 1.pdf");
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/YotaSpec02.pdf"); // √
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/1.pdf");
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/sample.pdf");
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/sig-notes.pdf");
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/sig-notes-new-txt.pdf");
			
			//if(transit){
			//	UIData.wdv.setImageReadyListener(() -> {
			//		root.post(this::closeSplashScreen);
			//		UIData.wdv.setImageReadyListener(null);
			//	});
			//}
			
			UIData.wdv.setImageReadyListener(() -> {
				root.post(() -> UIData.mainProgressBar.setVisibility(View.GONE));
				UIData.wdv.setImageReadyListener(null);
			});
			
			UIData.wdv.setDocumentPath("/sdcard/myFolder/sig-notes-t.pdf");
			//UIData.wdv.setDocumentPath("/sdcard/myFolder/sig-notes-new-txt-page0.pdf");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void closeSplashScreen() {
		root.setAlpha(0);
		ObjectAnimator fadeInContents = ObjectAnimator.ofFloat(root, "alpha", 0, 1);
		fadeInContents.setInterpolator(new AccelerateDecelerateInterpolator());
		fadeInContents.setDuration(350);
		fadeInContents.addListener(new Animator.AnimatorListener() {
			@Override public void onAnimationStart(Animator animation) { }
			@Override public void onAnimationEnd(Animator animation) {
				getWindow().setBackgroundDrawable(null);
			}
			@Override public void onAnimationCancel(Animator animation) { }
			@Override public void onAnimationRepeat(Animator animation) { }
		});
		root.post(fadeInContents::start);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(hidingContextMenu) {
			UIData.wdv.showContextMenuView();
			hidingContextMenu=false;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		UIData.wdv.checkDoc(false, true);
		if(hidingContextMenu) {
			UIData.wdv.hideContextMenuView();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		UIData.wdv.checkDoc(false, false);
	}
	
	public void OnMenuClicked(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ctx_copy:{
				String text = getSelection();
				if(text!=null) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
					clipboard.setPrimaryClip(ClipData.newPlainText("POLYM", text));
					showT("已复制！");
					UIData.wdv.clearSelection();
				}
			} break;
			case R.id.ctx_hightlight:{
				UIData.wdv.highlightSelection();
			} break;
			case R.id.ctx_enlarge:{
				UIData.wdv.enlargeSelection();
			} break;
			case R.id.ctx_share:{
				shareUrlOrText(getSelection());
			} break;
			case R.id.ctx_dictionay:{
				if(UIData.wdv.shouldDrawSelection()) {
					Intent intent = new Intent("colordict.intent.action.SEARCH");
					intent.putExtra("EXTRA_QUERY", getSelection());
					hidingContextMenu=true;
					startActivity(intent);
				}
			} break;
			case R.id.ctx_translation:{
				if(UIData.wdv.shouldDrawSelection()) {
					boolean processText = true;
					String Action=processText?Intent.ACTION_PROCESS_TEXT:Intent.ACTION_SEND;
					String Extra=processText?Intent.EXTRA_PROCESS_TEXT:Intent.EXTRA_TEXT;
					Intent intent = new Intent(Action);
					intent.setType("text/plain");
					try {
						intent.setPackage(GoogleTranslate);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(Extra, getSelection());
						startActivity(intent);
					} catch (Exception e) {
						showT(R.string.gt_no_inst);
					}
				}
			} break;
		}
	}
	
	private String getSelection() {
		String ret = UIData.wdv.getSelection();
		if(true && ret!=null) {
			ret = ret.replace("\r\n", " ");
		}
		return ret;
	}
	
	private void shareUrlOrText(String selection) {
		//CMN.Log("menu_icon6menu_icon6");
		//CMN.rt("分享链接……");
		int id = WeakReferenceHelper.share_dialog;
		BottomSheetDialog dlg = (BottomSheetDialog) getReferencedObject(id);
		if(dlg==null) {
			putReferencedObject(id, dlg=new AppIconsAdapter(this).shareDialog);
		}
		//CMN.pt("新建耗时：");
		AppIconsAdapter shareAdapter = (AppIconsAdapter) dlg.tag;
		shareAdapter.pullAvailableApps(this, null, selection);
		//CMN.pt("拉取耗时：");
	}
}
