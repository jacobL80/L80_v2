<?php
echo "PHP version: " . PHP_VERSION . "\n";
echo "PDO drivers: " . implode(', ', PDO::getAvailableDrivers()) . "\n";

require_once __DIR__ . '/config.php';

try {
    $pdo = new PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER,
        DB_PASS
    );
    echo "DB connected OK\n";
} catch (PDOException $e) {
    echo "DB error: " . $e->getMessage() . "\n";
}
