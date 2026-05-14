package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.fal.client.AsyncFalClient;
import ai.fal.client.ClientConfig;
import ai.fal.client.SubscribeOptions;
import com.google.gson.JsonObject;

public class DiscoverActivity extends AppCompatActivity {

    private LinearLayout chatMessagesContainer;
    private EditText chatInput;
    private AsyncFalClient falClient;
    private final List<ClothingItem> wardrobeInventory = new ArrayList<>();
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private LinearLayout selectedItemsContainer;
    private HorizontalScrollView selectedItemsScroll;
    private View outfitActionsContainer;
    private List<String> lastSelectedIds = new ArrayList<>();
    private DatabaseReference chatRef;
    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private boolean historyLoaded = false;

    private static final String SYSTEM_PROMPT = 
        "Role: You are \"VibeCheck,\" a high-end personal stylist with a deep mastery of modern fashion trends latest.\n\n" +
        "The Persona:\n" +
        "- Intellectual but Simple: Use sophisticated fashion terminology (e.g., \"monochrome,\" \"silhouette,\" \"texture contrast\") but keep your sentences short and punchy. No fluff.\n" +
        "- Trend-Savy: You know about current aesthetics like Quiet Luxury, Streetwear, Gorpcore, and Minimalism.\n" +
        "- Confident: Don't use \"I think\" or \"Maybe.\" Give definitive style advice like an expert.\n\n" +
        "Styling Logic:\n" +
        "- The Rule of Thirds: When suggesting outfits, focus on proportions.\n" +
        "- Color Theory: Suggest \"complementary\" or \"analogous\" colors. If an outfit is all one color, suggest different textures (e.g., \"Leather with knitwear\").\n" +
        "- The \"Third Piece\" Rule: Always suggest an accessory or an outer layer (jacket/bag) to \"complete\" the look.\n\n" +
        "Operational Directives:\n" +
        "- Tone: Act like a senior editor at a fashion magazine. Be helpful, direct, and slightly exclusive.\n" +
        "- Wardrobe Integration: Only select items from the provided [Wardrobe Inventory].\n" +
        "- Output Format: Provide the fashion advice in 2-3 short paragraphs, then end with the ID list: [SELECTED_ITEMS: ID_1, ID_2]. This tag is mandatory for visual recommendations.\n" +
        "- Outfit Modification: If a [CURRENTLY SELECTED OUTFIT] is provided in the prompt, DO NOT create a brand new outfit. Keep the base items the same and ONLY swap, add, or remove the specific items the user asked to change. Always output the final updated list in the [SELECTED_ITEMS: ID_1, ID_2] format.\n\n" +
        "CRITICAL: Never mention IDs in your speech. Provide the [SELECTED_ITEMS: ID_1, ID_2] tag at the very end of every styling response to ensure photos are displayed.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        chatMessagesContainer = findViewById(R.id.chat_messages_container);
        chatInput = findViewById(R.id.chat_input);
        ImageButton sendButton = findViewById(R.id.send_button);
        ImageButton historyButton = findViewById(R.id.history_button);
        View manualGenerateButton = findViewById(R.id.manual_generate_button);
        selectedItemsScroll = findViewById(R.id.selected_items_scroll);
        selectedItemsContainer = findViewById(R.id.selected_items_container);
        outfitActionsContainer = findViewById(R.id.outfit_actions_container);
        View btnClearSelection = findViewById(R.id.btn_clear_selection);

        if (selectedItemsContainer != null) {
            selectedItemsContainer.removeAllViews();
        }

        if (btnClearSelection != null) {
            btnClearSelection.setOnClickListener(v -> {
                lastSelectedIds.clear();
                updateSelectionTray(new ArrayList<>());
            });
        }

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        if (mAuth.getCurrentUser() != null) {
            chatRef = dbRef.child(mAuth.getCurrentUser().getUid()).child("discover_history");
        }

        loadWardrobeInventory();

        // Initialize FalClient using the key from BuildConfig
        try {
            falClient = AsyncFalClient.withConfig(
                ClientConfig.builder()
                    .withCredentials(() -> BuildConfig.FAL_KEY)
                    .build()
            );
        } catch (Exception e) {
            Log.e("DiscoverActivity", "Failed to initialize FalClient", e);
        }

        if (manualGenerateButton != null) {
            manualGenerateButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, AiActivity.class);
                startActivity(intent);
            });
        }

        View btnSchedule = findViewById(R.id.btn_schedule_outfit);
        if (btnSchedule != null) {
            btnSchedule.setOnClickListener(v -> showScheduleDialog());
        }

        View btnTryOn = findViewById(R.id.btn_try_on_ai);
        if (btnTryOn != null) {
            btnTryOn.setOnClickListener(v -> {
                ArrayList<ClothingItem> selectedItems = new ArrayList<>();
                for (String id : lastSelectedIds) {
                    ClothingItem item = findItemById(id);
                    if (item != null) {
                        selectedItems.add(item);
                    }
                }
                Intent intent = new Intent(this, AiActivity.class);
                intent.putExtra("SELECTED_ITEMS", selectedItems);
                startActivity(intent);
            });
        }

        if (historyButton != null) {
            historyButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Clear History")
                        .setMessage("Do you want to clear your chat history?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            if (chatRef != null) {
                                chatRef.removeValue();
                                chatMessagesContainer.removeAllViews();
                                // Restore greeting
                                LayoutInflater inflater = LayoutInflater.from(this);
                                View greeting = inflater.inflate(R.layout.item_chat_message_ai, chatMessagesContainer, false);
                                TextView tv = greeting.findViewById(R.id.message_text);
                                if (tv != null) {
                                    tv.setText(getString(R.string.ai_initial_greeting));
                                }
                                chatMessagesContainer.addView(greeting);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> sendMessage());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DiscoverActivity.this, D_FeedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadWardrobeInventory() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wardrobeInventory.clear();
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String catId = categorySnap.getKey();
                    DataSnapshot photosSnap = categorySnap.hasChild("photos") ? 
                            categorySnap.child("photos") : categorySnap;
                    
                    for (DataSnapshot photoSnap : photosSnap.getChildren()) {
                        if ("photos".equals(photoSnap.getKey())) continue;
                        
                        ClothingItem item = parseClothingItem(photoSnap, catId);
                        if (item != null) {
                            wardrobeInventory.add(item);
                        }
                    }
                }
                Log.d("DiscoverActivity", "Wardrobe loaded. Total items: " + wardrobeInventory.size());
                
                if (!historyLoaded && chatRef != null) {
                    loadChatHistory();
                    historyLoaded = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DiscoverActivity", "Failed to load wardrobe", error.toException());
            }
        });
    }

    private ClothingItem parseClothingItem(DataSnapshot photoSnap, String categoryId) {
        String id = photoSnap.getKey();
        String url = photoSnap.child("imageUrl").getValue(String.class);
        if (url == null) url = photoSnap.child("url").getValue(String.class);
        if (url == null) return null;

        ClothingItem item = new ClothingItem(id, url, categoryId);
        item.setName(photoSnap.child("name").getValue(String.class));

        Object seasonObj = photoSnap.child("season").getValue();
        if (seasonObj instanceof String) {
            item.setSeason(seasonObj.toString());
        } else if (seasonObj instanceof List) {
            List<?> seasons = (List<?>) seasonObj;
            if (!seasons.isEmpty()) {
                item.setSeason(seasons.get(0).toString());
            }
        }

        Object colorObj = photoSnap.child("colors").getValue();
        String color = null;
        if (colorObj instanceof String) {
            color = (String) colorObj;
        } else if (colorObj instanceof List) {
            List<?> colors = (List<?>) colorObj;
            if (!colors.isEmpty() && colors.get(0) instanceof String) {
                color = (String) colors.get(0);
            }
        } else if (photoSnap.child("color").exists()) {
            color = photoSnap.child("color").getValue(String.class);
        }
        item.setColor(color);

        item.setSize(photoSnap.child("size").getValue(String.class));

        DataSnapshot occasionsSnap = photoSnap.child("occasions");
        List<String> occasionsList = new ArrayList<>();
        if (occasionsSnap.exists()) {
            Object occasionsObj = occasionsSnap.getValue();
            if (occasionsObj instanceof List) {
                for (DataSnapshot occasion : occasionsSnap.getChildren()) {
                    String val = occasion.getValue(String.class);
                    if (val != null) occasionsList.add(val);
                }
            } else if (occasionsObj instanceof String) {
                occasionsList.add((String) occasionsObj);
            }
        }
        item.setOccasions(occasionsList);

        return item;
    }

    private String getInventoryContext() {
        if (wardrobeInventory.isEmpty()) {
            return "\n\n[Wardrobe Inventory]: (Empty. Advise the user to add clothes to their wardrobe first.)";
        }
        StringBuilder sb = new StringBuilder("\n\n[Wardrobe Inventory]:\n");
        for (ClothingItem item : wardrobeInventory) {
            sb.append("- ID: ").append(item.getId())
              .append(", Name: ").append(item.getName() != null ? item.getName() : "Clothing Item")
              .append(", Category: ").append(item.getCategoryId())
              .append(", Color: ").append(item.getColor() != null ? item.getColor() : "Unknown")
              .append(", Season: ").append(item.getSeason() != null ? item.getSeason() : "All")
              .append(", Occasions: ").append(item.getOccasions() != null && !item.getOccasions().isEmpty() ? String.join(", ", item.getOccasions()) : "Any")
              .append("\n");
        }
        return sb.toString();
    }

    private void loadChatHistory() {
        chatRef.orderByChild("timestamp").limitToLast(50).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    chatMessagesContainer.removeAllViews();
                    conversationHistory.clear();
                }
                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    ChatMessage msg = chatSnap.getValue(ChatMessage.class);
                    if (msg != null) {
                        displayMessage(msg);
                        conversationHistory.add(msg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DiscoverActivity", "Failed to load chat history", error.toException());
            }
        });
    }

    private void displayMessage(ChatMessage msg) {
        String message = msg.getText();
        boolean isAi = msg.isAi();
        List<String> selectedIds = msg.getSelectedItemIds();

        LayoutInflater inflater = LayoutInflater.from(this);
        View messageLayout = inflater.inflate(
                isAi ? R.layout.item_chat_message_ai : R.layout.item_chat_message_user,
                chatMessagesContainer,
                false
        );

        TextView textView = messageLayout.findViewById(R.id.message_text);
        LinearLayout imagesContainer = messageLayout.findViewById(R.id.message_images_container);
        HorizontalScrollView imagesScroll = messageLayout.findViewById(R.id.message_images_scroll);
        com.google.android.material.button.MaterialButton selectBtn = messageLayout.findViewById(R.id.btn_select_outfit);

        textView.setText(message);

        if (selectedIds != null && !selectedIds.isEmpty()) {
            if (imagesScroll != null && imagesContainer != null) {
                imagesScroll.setVisibility(View.VISIBLE);
                imagesContainer.removeAllViews();
                for (String id : selectedIds) {
                    ClothingItem item = findItemById(id);
                    if (item != null) {
                        addImageToMessage(item, imagesContainer);
                    }
                }
            }

            if (isAi && selectBtn != null) {
                selectBtn.setVisibility(View.VISIBLE);
                selectBtn.setOnClickListener(v -> {
                    updateSelectionTray(selectedIds);
                    Toast.makeText(this, "Outfit applied to selection tray", Toast.LENGTH_SHORT).show();
                    
                    selectBtn.setEnabled(false);
                    selectBtn.setText("Outfit Selected");
                    selectBtn.setIconResource(R.drawable.ic_check_badge);
                });
            }
        }

        chatMessagesContainer.addView(messageLayout);

        View scrollView = (View) chatMessagesContainer.getParent();
        if (scrollView instanceof android.widget.ScrollView) {
            scrollView.post(() -> ((android.widget.ScrollView) scrollView).fullScroll(View.FOCUS_DOWN));
        }
    }

    private void sendMessage() {
        String message = chatInput.getText().toString().trim();
        if (message.isEmpty()) return;

        chatInput.setText("");
        saveAndDisplayMessage(message, false, new ArrayList<>());

        if (falClient == null) {
            Toast.makeText(this, "AI not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (wardrobeInventory.isEmpty()) {
            Toast.makeText(this, "Add items to your wardrobe first to get styling advice with photos!", Toast.LENGTH_LONG).show();
        }

        String inventoryContext = getInventoryContext();
        List<String> currentOutfitIds = new ArrayList<>();
        for (int i = conversationHistory.size() - 2; i >= 0; i--) {
            ChatMessage m = conversationHistory.get(i);
            if (m.isAi() && m.getSelectedItemIds() != null && !m.getSelectedItemIds().isEmpty()) {
                currentOutfitIds = m.getSelectedItemIds();
                break;
            }
        }

        StringBuilder contextualPrompt = new StringBuilder(message);
        if (!currentOutfitIds.isEmpty()) {
            contextualPrompt.append("\n\n[CURRENTLY SELECTED OUTFIT]: ");
            List<String> itemDescriptions = new ArrayList<>();
            for (String id : currentOutfitIds) {
                ClothingItem item = findItemById(id);
                if (item != null) {
                    itemDescriptions.add(id + " (" + (item.getName() != null ? item.getName() : item.getCategoryId()) + ")");
                } else {
                    itemDescriptions.add(id);
                }
            }
            contextualPrompt.append(String.join(", ", itemDescriptions));
        }

        StringBuilder finalPrompt = new StringBuilder();
        int historyStart = Math.max(0, conversationHistory.size() - 7);
        for (int i = historyStart; i < conversationHistory.size() - 1; i++) {
            ChatMessage m = conversationHistory.get(i);
            finalPrompt.append(m.isAi() ? "Stylist: " : "User: ").append(m.getText());
            if (m.isAi() && m.getSelectedItemIds() != null && !m.getSelectedItemIds().isEmpty()) {
                finalPrompt.append(" [SELECTED_ITEMS: ").append(String.join(", ", m.getSelectedItemIds())).append("]");
            }
            finalPrompt.append("\n");
        }
        finalPrompt.append("User: ").append(contextualPrompt);

        Map<String, Object> input = Map.of(
            "image_urls", new ArrayList<>(),
            "prompt", finalPrompt.toString(),
            "system_prompt", SYSTEM_PROMPT + inventoryContext,
            "model", "google/gemini-2.0-flash-001"
        );

        falClient.subscribe("openrouter/router/vision",
            SubscribeOptions.<JsonObject>builder()
                .input(input)
                .resultType(JsonObject.class)
                .build()
        ).whenComplete((result, throwable) -> {
            runOnUiThread(() -> {
                if (throwable != null) {
                    Toast.makeText(this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (result != null && result.getData().has("output")) {
                    String aiResponse = result.getData().get("output").getAsString();
                    List<String> selectedIds = new ArrayList<>();
                    String displayMessage = aiResponse;
                    Pattern pattern = Pattern.compile("(?i)\\[SELECTED_ITEMS:\\s*([^]]+)]");
                    Matcher matcher = pattern.matcher(aiResponse);
                    if (matcher.find()) {
                        String idsString = matcher.group(1);
                        if (idsString != null) {
                            for (String id : idsString.split(",\\s*")) {
                                selectedIds.add(id.trim());
                            }
                        }
                        displayMessage = matcher.replaceAll("").trim();
                    }
                    saveAndDisplayMessage(displayMessage, true, selectedIds);
                }
            });
        });
    }

    private void saveAndDisplayMessage(String text, boolean isAi, List<String> selectedIds) {
        View initialGreeting = findViewById(R.id.initial_greeting);
        if (initialGreeting != null) {
            chatMessagesContainer.removeView(initialGreeting);
        }

        ChatMessage msg = new ChatMessage(text, isAi, System.currentTimeMillis(), selectedIds);
        conversationHistory.add(msg);
        if (chatRef != null) {
            chatRef.push().setValue(msg);
        }
        displayMessage(msg);
    }

    private void showScheduleDialog() {
        if (lastSelectedIds.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTitle = dialogView.findViewById(R.id.et_event_title);
        EditText etDate = dialogView.findViewById(R.id.et_event_date);
        EditText etTime = dialogView.findViewById(R.id.et_event_time);
        View btnSave = dialogView.findViewById(R.id.btn_save_event);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_event);

        etDate.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String date = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etDate.setText(date);
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String time = String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                etTime.setText(time);
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), false).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            saveOutfitEvent(title, date, time);
            dialog.dismiss();
            Toast.makeText(this, "Outfit scheduled!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void saveOutfitEvent(String title, String date, String time) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference eventsRef = dbRef.child(uid).child("Events");

        String eventId = eventsRef.push().getKey();
        if (eventId == null) return;

        List<ClothingItem> selectedItems = new ArrayList<>();
        for (String id : lastSelectedIds) {
            ClothingItem item = findItemById(id);
            if (item != null) selectedItems.add(item);
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", eventId);
        eventData.put("title", title);
        eventData.put("date", date);
        eventData.put("time", time);
        eventData.put("items", selectedItems);

        eventsRef.child(eventId).setValue(eventData);
    }

    private void addImageToMessage(ClothingItem item, LinearLayout container) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_chat_outfit_preview, container, false);
        ImageView imageView = itemView.findViewById(R.id.item_image);

        Glide.with(this)
            .load(item.getImageUrl())
            .into(imageView);

        itemView.setOnClickListener(v -> {
            Toast.makeText(this, (item.getName() != null ? item.getName() : "Clothing Item"), Toast.LENGTH_SHORT).show();
        });

        container.addView(itemView);
    }

    private void resetAllSelectButtons() {
        if (chatMessagesContainer == null) return;
        for (int i = 0; i < chatMessagesContainer.getChildCount(); i++) {
            View messageView = chatMessagesContainer.getChildAt(i);
            com.google.android.material.button.MaterialButton selectBtn = messageView.findViewById(R.id.btn_select_outfit);
            if (selectBtn != null) {
                selectBtn.setEnabled(true);
                selectBtn.setText("Select this Outfit");
                selectBtn.setIconResource(R.drawable.magic);
            }
        }
    }

    private void updateSelectionTray(List<String> itemIds) {
        resetAllSelectButtons();
        selectedItemsContainer.removeAllViews();
        lastSelectedIds = new ArrayList<>(itemIds);
        boolean hasItems = false;

        for (String id : itemIds) {
            ClothingItem item = findItemById(id.trim());
            if (item != null) {
                addItemToTray(item);
                hasItems = true;
            }
        }

        View btnClearSelection = findViewById(R.id.btn_clear_selection);
        View trayContainer = findViewById(R.id.selection_tray_container);
        
        if (hasItems) {
            if (trayContainer != null) trayContainer.setVisibility(View.VISIBLE);
            selectedItemsScroll.setVisibility(View.VISIBLE);
            if (outfitActionsContainer != null) outfitActionsContainer.setVisibility(View.VISIBLE);
            if (btnClearSelection != null) btnClearSelection.setVisibility(View.VISIBLE);
        } else {
            if (trayContainer != null) trayContainer.setVisibility(View.GONE);
            selectedItemsScroll.setVisibility(View.GONE);
            if (outfitActionsContainer != null) outfitActionsContainer.setVisibility(View.GONE);
            if (btnClearSelection != null) btnClearSelection.setVisibility(View.GONE);
        }
    }

    private ClothingItem findItemById(String id) {
        for (ClothingItem item : wardrobeInventory) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    private void addItemToTray(ClothingItem item) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_selection_tray, selectedItemsContainer, false);
        ImageView imageView = itemView.findViewById(R.id.item_image);

        Glide.with(this)
            .load(item.getImageUrl())
            .into(imageView);

        itemView.setOnClickListener(v -> {
            Toast.makeText(this, "Item: " + (item.getCategoryId() != null ? item.getCategoryId() : "Clothing Item"), Toast.LENGTH_SHORT).show();
        });

        selectedItemsContainer.addView(itemView);
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D_FeedActivity.class);
        } else if (viewId == R.id.wardrobe_menu) {
            intent = new Intent(this, WardrobeActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E_CalendarActivity.class);
        } else if (viewId == R.id.discover_menu) {
            return;
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I_ProfileActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
