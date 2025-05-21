# GreenCode Challenge

App Android para visualiar conversiones de monedas en tiempo real. Permite seleccionar monedas de origen y destino, con tasas de cambio actualizadas. Desarrollada con MVVM y Clean Architecture.

## Para ejecutar

1. Clona el repo
2. Crea `local.properties` con:
```properties
API_KEY=tu_api_key
BASE_URL=https://api.exchangerate.host/
```
3. Sincroniza y ejecuta

## 🛠 Dependencias principales

- Kotlin + Coroutines
- Hilt (DI)
- Retrofit + Room
- Material Design

## 💡 Decisiones técnicas

- MVVM + Clean Architecture para separar responsabilidades y facilitar testing
- Coroutines + Flow para manejar operaciones asíncronas y actualizaciones en tiempo real
- Repository pattern para centralizar la lógica de obtención de tasas de cambio
- ViewBinding para una interacción más segura con las vistas
- StateFlow para manejar estados de UI y actualizaciones de conversiones

## 🔜 Mejoras futuras

### UX/UI
- Mejorar la UI con iconos de los paises a los que pertenece la moneda elegida
- Mejorar el formato del input del monto usando TextWatcher
- Hacer dinámico el resultado a medida que se ingresan valores
- Setear un monto mínimo según las tasas de conversión

### Internacionalización
- Agregar soporte a más idiomas usando Lokalise

### Calidad
- Agregar más tests unitarios
- Mejorar manejo de errores
- Agregar test automatizados de UI con Espresso
