module.exports = {
  env: {
    es6: true,
    node: true,
    mocha: true, // If you need Mocha for your tests
  },
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: "module", // Ensures ES modules are handled correctly
  },
  extends: [
    "eslint:recommended",
    "google",
  ],
  rules: {
    "no-restricted-globals": ["error", "name", "length"],
    "prefer-arrow-callback": "error",
    "quotes": ["error", "double", {"allowTemplateLiterals": true}],
    "max-len": ["error", {code: 140}],
  },
  overrides: [
    {
      files: ["**/*.spec.*"],
      env: {
        mocha: true, // Overrides for test files
      },
      rules: {},
    },
  ],
};
