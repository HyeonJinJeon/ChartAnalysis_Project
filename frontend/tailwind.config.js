/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts}'],
  theme: {
    extend: {
      colors: {
        bg: {
          primary: '#0d0d0d',
          secondary: '#1a1a1a',
          tertiary: '#242424',
          card: '#1e1e1e',
        },
        border: {
          DEFAULT: '#2a2a2a',
          light: '#333333',
        },
        up: '#f04251',
        down: '#4b8cf7',
        text: {
          primary: '#ffffff',
          secondary: '#9e9e9e',
          muted: '#666666',
        },
        toss: {
          blue: '#3182f6',
          red: '#f04251',
        }
      },
      fontFamily: {
        sans: ['Pretendard', 'system-ui', 'sans-serif'],
      }
    }
  },
  plugins: []
}
