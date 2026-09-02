package com.twistedfantasy.uniqueweapons.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.twistedfantasy.uniqueweapons.items.FallingBook;
import com.twistedfantasy.uniqueweapons.network.PacketHandler;
import com.twistedfantasy.uniqueweapons.network.PacketHandler.SelectAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

public class AbilitySelectionScreen extends Screen {
    
    private final FallingBook book;
    private final ItemStack stack;
    private int centerX;
    private int centerY;
    private int radius = 90;
    private int buttonSize = 36;
    private int hoveredAbility = -1;
    
    private static final String[] ABILITY_NAMES = {
        "§d§lБарьер",
        "§d§lИзвращённая\n§d§lтехника", 
        "§d§lВоплощение\n§d§lповелителей",
        "§d§lРазвеивание",
        "§d§lПроклятие\n§d§lНебес"
    };
    private static final String[] ABILITY_DESCRIPTIONS = {
        "§dБарьер\n\n§7Создаёт вокруг себя защитное поле на 30 секунд\n§7● Отталкивает врагов и снаряды\n§7● Радиус: 8 блоков\n§7● Наносит 2 урона при контакте\n§7● Даёт Сопротивление II\n§7● Даёт 6 сердец поглощения\n§7● Уничтожает снаряды\n\n§6Перезарядка: §e60 секунд",
        "§dИзвращённая техника\n\n§7Следующий полученный урон\n§7возвращается всем врагам\n§7● Радиус: 50 блоков\n§7● Возвращает 150% урона\n§7● Работает один раз\n§7● Затем требуется перезарядка\n\n§6Перезарядка: §e40 секунд",
        "§dВоплощение повелителей\n\n§7Мощное усиление на 15 секунд\n§7● Здоровье ×3\n§7● +20 сердец поглощения\n§7● Сопротивление II\n§7● Регенерация III\n§7● Сила II (+8 к урону)\n§7● Огнестойкость\n§7● Скорость II\n§7● Прыжок II\n§7● Полное исцеление при активации\n\n§6Перезарядка: §e180 секунд",
        "§dНевозможная техника: Развеивание\n\n§7Мгновенно снимает все эффекты\n§7с врагов и наносит урон\n§7● Радиус: 30 блоков\n§7● Снимает ВСЕ эффекты\n§7● Урон = 50% текущего здоровья\n§7● Не влияет на союзников\n\n§6Перезарядка: §e45 секунд",
        "§dНевозможная техника: Проклятие Небес\n\n§7Накладывает абсолютное проклятие\n§7на выбранную цель\n§7● Длительность: 10 секунд\n§7● Полная неподвижность\n§7● Невозможность атаковать\n§7● Блокировка элитр\n§7● Замедление копания\n§7● Поиск цели по взгляду\n\n§6Перезарядка: §e90 секунд"
    };
    
    private static final int[] ABILITY_COOLDOWNS = {60, 40, 180, 45, 90};
    
    public AbilitySelectionScreen(FallingBook book, ItemStack stack) {
        super(new TextComponent(""));
        this.book = book;
        this.stack = stack;
    }
    
    @Override
    protected void init() {
        super.init();
        
        centerX = this.width / 2;
        centerY = this.height / 2;
        
         
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {  
            int ability = getAbilityFromDirection(mouseX, mouseY);
            if (ability != -1) {
                selectAbility(ability);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private int getAbilityFromDirection(double mouseX, double mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
         
        if (distance < 20) {
            return -1;
        }
        
         
        double angle = Math.atan2(dy, dx);
        
         
        if (angle < 0) angle += 2 * Math.PI;
        
         
        angle += Math.PI / 2;
        if (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        
         
        double sectorSize = 2 * Math.PI / 5;
        
        for (int i = 0; i < 5; i++) {
            double sectorStart = i * sectorSize;
            double sectorEnd = (i + 1) * sectorSize;
            
             
            if (angle >= sectorStart && angle < sectorEnd) {
                return i + 1;  
            }
        }
        
         
        if (angle >= 5 * sectorSize) {
            return 1;  
        }
        
        return -1;
    }
    
    private void selectAbility(int abilityId) {
        PacketHandler.INSTANCE.sendToServer(new SelectAbilityPacket(abilityId));
        this.onClose();
    }
    
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        
        drawCenteredString(poseStack, this.font, "§d§lКнига Падения - Выбор способности", 
            centerX, centerY - radius - 50, 0xFFFFFF);
        
        drawCenteredString(poseStack, this.font, "§7Нажмите в направлении нужной способности", 
            centerX, centerY - radius - 35, 0xAAAAAA);
        
        super.render(poseStack, mouseX, mouseY, partialTicks);
        
        drawAbilityNames(poseStack);
        
        drawCenteredString(poseStack, this.font, "§7ESC - закрыть  |  Нажмите в сторону способности для выбора", 
            centerX, centerY + radius + 40, 0x888888);
        
         
        int pointedAbility = getAbilityFromDirection(mouseX, mouseY);
        if (pointedAbility != -1) {
             
            drawSelectedSector(poseStack, pointedAbility);
            
             
            String[] lines = ABILITY_DESCRIPTIONS[pointedAbility - 1].split("\n");
            int boxY = mouseY + 15;
            int boxWidth = 0;
            
            for (String line : lines) {
                int lineWidth = this.font.width(line);
                if (lineWidth > boxWidth) boxWidth = lineWidth;
            }
            
            int boxX = mouseX + 15;
            if (boxX + boxWidth + 10 > this.width) {
                boxX = mouseX - boxWidth - 15;
            }
            
            fill(poseStack, boxX - 5, boxY - 5, boxX + boxWidth + 5, boxY + lines.length * 10 + 5, 0xDD000000);
            
            for (int i = 0; i < lines.length; i++) {
                this.font.draw(poseStack, lines[i], boxX, boxY + i * 10, 0xFFFFFF);
            }
        } else {
            drawCenteredString(poseStack, this.font, "§7Наведите в сторону способности для подробного описания", 
                centerX, centerY + radius + 55, 0x888888);
        }
    }
    
    private void drawSelectedSector(PoseStack poseStack, int ability) {
        int abilityIndex = ability - 1;
        double sectorSize = 2 * Math.PI / 5;
        double startAngle = abilityIndex * sectorSize - Math.PI / 2;
        double endAngle = (abilityIndex + 1) * sectorSize - Math.PI / 2;
        
         
        for (int r = radius - 25; r <= radius + 15; r++) {
            for (int a = 0; a < 20; a++) {
                double angle = startAngle + a * (endAngle - startAngle) / 19;
                int x = centerX + (int)(r * Math.cos(angle));
                int y = centerY + (int)(r * Math.sin(angle));
                
                int color = 0x30FFFFFF;
                if (r <= radius - 15) color = 0x10FFFFFF;
                else if (r >= radius + 5) color = 0x10FFFFFF;
                
                fill(poseStack, x, y, x + 1, y + 1, color);
            }
        }
    }
    
    private void drawAbilityNames(PoseStack poseStack) {
        for (int i = 0; i < 5; i++) {
            double angle = 2 * Math.PI * i / 5 - Math.PI / 2;
            int x = (int)(centerX + (radius + 25) * Math.cos(angle));
            int y = (int)(centerY + (radius + 25) * Math.sin(angle));
            
            String[] lines = ABILITY_NAMES[i].split("\n");
            for (int j = 0; j < lines.length; j++) {
                drawCenteredString(poseStack, this.font, lines[j], 
                    x, y - (lines.length - 1) * 5 + j * 10, 0xFFFFFF);
            }
            
            drawCenteredString(poseStack, this.font, "§8" + ABILITY_COOLDOWNS[i] + "с", 
                x, y + 15, 0xAAAAAA);
        }
    }
    
    @Override
    public void renderBackground(PoseStack poseStack) {
        this.fillGradient(poseStack, 0, 0, this.width, this.height, 0x90000000, 0xC0000000);
        
         
        for (int i = 0; i < 5; i++) {
            double angle = 2 * Math.PI * i / 5 - Math.PI / 2;
            int x = (int)(centerX + radius * Math.cos(angle));
            int y = (int)(centerY + radius * Math.sin(angle));
            
            drawConnectingLine(poseStack, centerX, centerY, x, y, 0x30FFFFFF);
        }
        
         
        drawCircleOutline(poseStack, centerX, centerY, radius + 15, 0x40FFFFFF);
        drawCircleOutline(poseStack, centerX, centerY, radius - 25, 0x40FFFFFF);
        
         
        for (int i = 0; i < 5; i++) {
            double angle = 2 * Math.PI * i / 5 - Math.PI / 2;
            int x1 = (int)(centerX + (radius - 25) * Math.cos(angle));
            int y1 = (int)(centerY + (radius - 25) * Math.sin(angle));
            int x2 = (int)(centerX + (radius + 15) * Math.cos(angle));
            int y2 = (int)(centerY + (radius + 15) * Math.sin(angle));
            
            drawLine(poseStack, x1, y1, x2, y2, 0x40FFFFFF);
        }
    }
    
    private void drawConnectingLine(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        drawLine(poseStack, x1, y1, x2, y2, color);
    }
    
    private void drawLine(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            fill(poseStack, x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
    
    private void drawCircleOutline(PoseStack poseStack, int cx, int cy, int radius, int color) {
        int x = radius - 1;
        int y = 0;
        int dx = 1;
        int dy = 1;
        int err = dx - (radius << 1);
        
        while (x >= y) {
            drawCirclePoint(poseStack, cx + x, cy + y, color);
            drawCirclePoint(poseStack, cx + y, cy + x, color);
            drawCirclePoint(poseStack, cx - y, cy + x, color);
            drawCirclePoint(poseStack, cx - x, cy + y, color);
            drawCirclePoint(poseStack, cx - x, cy - y, color);
            drawCirclePoint(poseStack, cx - y, cy - x, color);
            drawCirclePoint(poseStack, cx + y, cy - x, color);
            drawCirclePoint(poseStack, cx + x, cy - y, color);
            
            if (err <= 0) {
                y++;
                err += dy;
                dy += 2;
            }
            if (err > 0) {
                x--;
                dx += 2;
                err += dx - (radius << 1);
            }
        }
    }
    
    private void drawCirclePoint(PoseStack poseStack, int x, int y, int color) {
        fill(poseStack, x, y, x + 1, y + 1, color);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {  
            this.onClose();
            return true;
        }
        
        if (keyCode >= 49 && keyCode <= 53) {  
            int abilityId = keyCode - 48;
            selectAbility(abilityId);
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}