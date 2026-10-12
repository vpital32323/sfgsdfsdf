# Iwoho Launcher — Iwoho Visuals

## Запуск
1. Установи Node.js 18+ и Java 17+.
2. В папке проекта: `npm install` затем `npm start`.
3. Сборка .exe: `npm run build` (результат в `dist/`).

## Свой логотип
Замени `assets/logo.svg` на свой файл (или поменяй путь `../assets/logo.svg` в `src/splash.html` и `src/index.html`).
Для иконки .exe положи `assets/icon.ico` (256x256).

## Обновить репозиторий на GitHub
Через сайт: открой репозиторий → Add file → Upload files → перетащи содержимое папки (с заменой) → Commit changes.
Через терминал:
    git add .
    git commit -m "Iwoho Launcher: новый дизайн"
    git push
