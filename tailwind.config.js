/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/resources/static/js/**/*.js",
  ],
  theme: {
    extend: {
      colors: {
        // 기존 디자인 시스템 색상 유지
        'mindlog-dark': {
          '50': '#0f3460',
          '100': '#16213e',
          '200': '#1a1a2e',
        },
        'mindlog-purple': {
          '400': '#667eea',
          '500': '#764ba2',
        },
        'mindlog-text': {
          'primary': '#e6f1ff',
          'secondary': '#8892b0',
        },
      },
      fontFamily: {
        sans: ['Noto Sans KR', 'sans-serif'],
      },
    },
  },
  plugins: [
    require('preline/plugin'),
  ],
}
