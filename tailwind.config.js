/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/cljs/**/*.cljs",
    "./resources/public/index.html"
  ],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
  plugins: [],
}
