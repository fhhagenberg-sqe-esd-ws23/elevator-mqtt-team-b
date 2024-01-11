FROM python:3.9.18-slim-bookworm
COPY ./ /gui
EXPOSE 8000
CMD ["python", "-m", "http.server", "--directory", "/gui"]
