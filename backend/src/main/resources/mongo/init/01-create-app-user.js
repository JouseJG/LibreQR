// Crea el usuario de aplicación de LibreQR de forma idempotente.
// Se ejecuta solo en el arranque inicial del contenedor (cuando /data/db está vacío),
// con las credenciales root disponibles en MONGO_INITDB_* del entrypoint oficial.
const dbName = process.env.MONGO_DB || 'libreqr';
const user = process.env.MONGO_USER || 'libreqr';
const pwd = process.env.MONGO_PASSWORD || 'libreqrPassword123!';

const target = db.getSiblingDB(dbName);

const existing = target.getUser(user);
if (existing) {
  print(`[init] Usuario de aplicación '${user}' ya existe en '${dbName}'. Sin cambios.`);
} else {
  target.createUser({
    user: user,
    pwd: pwd,
    roles: [{ role: 'readWrite', db: dbName }]
  });
  print(`[init] Usuario de aplicación '${user}' creado en '${dbName}' (rol readWrite).`);
}
